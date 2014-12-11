package tv.mediabrowser.mediabrowsertv;

import java.io.IOException;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserItemDataDto;


public class BaseItemDetailsFragment extends DetailsFragment {
    private static final String TAG = "BaseItemDetailsFragment";

    private static final int ACTION_PLAY = 1;
    private static final int ACTION_RESUME = 2;
    private static final int ACTION_DETAILS = 3;

    private static final int DETAIL_THUMB_WIDTH = 150;
    private static final int DETAIL_THUMB_HEIGHT = 150;

    private static final int NUM_COLS = 10;

    private BaseItemDto mBaseItem;
    private ApiClient mApiClient;

    private Drawable mDefaultBackground;
    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private DetailsOverviewRowPresenter mDorPresenter;
    private DetailRowBuilderTask mDetailRowBuilderTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);

        mDorPresenter =
                new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mBaseItem = TvApp.getApplication().getSerializer().DeserializeFromString(getActivity().getIntent().getStringExtra("BaseItemDto"),BaseItemDto.class);
        mDetailRowBuilderTask = (DetailRowBuilderTask) new DetailRowBuilderTask().execute(mBaseItem);
        mDorPresenter.setSharedElementEnterTransition(getActivity(),
                DetailsActivity.SHARED_ELEMENT_NAME);

        mApiClient = TvApp.getApplication().getApiClient();

        updateBackground(Utils.getBackdropImageUrl(mBaseItem, TvApp.getApplication().getApiClient()));
        setOnItemViewClickedListener(new ItemViewClickedListener());

    }

    @Override
    public void onStop() {
        mDetailRowBuilderTask.cancel(true);
        super.onStop();
    }

    private class DetailRowBuilderTask extends AsyncTask<BaseItemDto, Integer, DetailsOverviewRow> {
        @Override
        protected DetailsOverviewRow doInBackground(BaseItemDto... baseItem) {
            BaseItemDetailsFragment.this.mBaseItem = baseItem[0];

            DetailsOverviewRow row = new DetailsOverviewRow(BaseItemDetailsFragment.this.mBaseItem);
            try {
                Bitmap poster = Picasso.with(getActivity())
                        .load(Utils.getPrimaryImageUrl(BaseItemDetailsFragment.this.mBaseItem, mApiClient, true))
                                .resize(Utils.convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_WIDTH),
                                        Utils.convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_HEIGHT))
                                .centerInside()
                                .get();
                row.setImageBitmap(getActivity(), poster);
            } catch (IOException e) {
            }

            UserItemDataDto userData = BaseItemDetailsFragment.this.mBaseItem.getUserData();
            if (userData != null && userData.getPlaybackPositionTicks() > 0) {
                row.addAction(new Action(ACTION_RESUME, "Resume"));
            }

            row.addAction(new Action(ACTION_PLAY, "Play", "From Beginning"));
            row.addAction(new Action(ACTION_DETAILS, "Full Details"));
            return row;
        }

        @Override
        protected void onPostExecute(DetailsOverviewRow detailRow) {
            ClassPresenterSelector ps = new ClassPresenterSelector();
            // set detail background and style
            mDorPresenter.setBackgroundColor(getResources().getColor(R.color.detail_background));
            mDorPresenter.setStyleLarge(true);
            mDorPresenter.setOnActionClickedListener(new OnActionClickedListener() {
                @Override
                public void onActionClicked(Action action) {
                    if (action.getId() == ACTION_PLAY) {
                        Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);
                        String[] items = new String[] {TvApp.getApplication().getSerializer().SerializeToString(mBaseItem)};
                        intent.putExtra("Items", items);
                        intent.putExtra("ShouldStart", true);
                        startActivity(intent);
                    } else {
                        if (action.getId() == ACTION_RESUME) {
                            Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);
                            String[] items = new String[] {TvApp.getApplication().getSerializer().SerializeToString(mBaseItem)};
                            intent.putExtra("Items", items);
                            intent.putExtra("ShouldStart", true);
                            Long pos = mBaseItem.getUserData().getPlaybackPositionTicks() / 10000;
                            intent.putExtra("Position", pos.intValue());
                            startActivity(intent);

                        } else {
                            Toast.makeText(getActivity(), action.toString() + " not implemented", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

            ps.addClassPresenter(DetailsOverviewRow.class, mDorPresenter);
            ps.addClassPresenter(ListRow.class,
                    new ListRowPresenter());

            ArrayObjectAdapter adapter = new ArrayObjectAdapter(ps);
            adapter.add(detailRow);

//            String subcategories[] = {
//                    getString(R.string.related_movies)
//            };
//            List<Movie> list = MovieList.list;
//            Collections.shuffle(list);
//            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
//            for (int j = 0; j < NUM_COLS; j++) {
//                listRowAdapter.add(list.get(j % 5));
//            }
//
//            HeaderItem header = new HeaderItem(0, subcategories[0], null);
//            adapter.add(new ListRow(header, listRowAdapter));

            setAdapter(adapter);
        }

    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Movie) {
                Movie movie = (Movie) item;
                Log.d(TAG, "Item: " + item.toString());
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(DetailsActivity.MOVIE, movie);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }

    protected void updateBackground(String url) {
        Log.d(TAG, "url" + url);
        Log.d(TAG, "metrics" + mMetrics.toString());
        Picasso.with(getActivity())
                .load(url)
                .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                .error(mDefaultBackground)
                .into(mBackgroundTarget);
    }

}
