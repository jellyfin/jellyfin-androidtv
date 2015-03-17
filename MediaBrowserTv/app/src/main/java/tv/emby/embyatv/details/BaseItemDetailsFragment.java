package tv.emby.embyatv.details;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

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
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ChapterInfoDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.livetv.ProgramInfoDto;
import mediabrowser.model.livetv.SeriesTimerInfoDto;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.NextUpQuery;
import mediabrowser.model.querying.SeasonQuery;
import mediabrowser.model.querying.SimilarItemsQuery;
import mediabrowser.model.querying.UpcomingEpisodesQuery;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.model.ChapterItemInfo;
import tv.emby.embyatv.itemhandling.ItemLauncher;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.imagehandling.PicassoBackgroundManagerTarget;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.playback.PlaybackOverlayActivity;
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.presentation.DetailsDescriptionPresenter;
import tv.emby.embyatv.querying.QueryType;
import tv.emby.embyatv.querying.SpecialsQuery;
import tv.emby.embyatv.querying.TrailersQuery;
import tv.emby.embyatv.util.Utils;


public class BaseItemDetailsFragment extends DetailsFragment {
    private static final String TAG = "BaseItemDetailsFragment";

    private static final int ACTION_PLAY = 1;
    private static final int ACTION_RESUME = 2;
    private static final int ACTION_DETAILS = 3;
    private static final int ACTION_SHUFFLE = 4;
    private static final int ACTION_RECORD = 5;
    private static final int ACTION_CANCEL_RECORD = 6;

    private static final int DETAIL_THUMB_WIDTH = 150;
    private static final int DETAIL_THUMB_HEIGHT = 200;

    protected BaseItemDto mBaseItem;
    protected ProgramInfoDto mProgramInfo;
    protected String mItemId;
    protected String mChannelId;
    protected ApiClient mApiClient;
    protected DetailsActivity mActivity;
    protected TvApp mApplication;
    protected Calendar lastLoaded = Calendar.getInstance();

    private Drawable mDefaultBackground;
    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private DetailsOverviewRowPresenter mDorPresenter;
    private DetailRowBuilderTask mDetailRowBuilderTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);
        mApplication = TvApp.getApplication();
        mApiClient = mApplication.getApiClient();
        //Make sure we refresh
        lastLoaded.set(Calendar.YEAR, 2000);

        mDorPresenter =
                new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);

        mMetrics = new DisplayMetrics();
        mActivity = (DetailsActivity) getActivity();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mItemId = mActivity.getIntent().getStringExtra("ItemId");
        mChannelId = mActivity.getIntent().getStringExtra("ChannelId");
        String programJson = mActivity.getIntent().getStringExtra("ProgramInfo");
        if (programJson != null) mProgramInfo = mApplication.getSerializer().DeserializeFromString(programJson, ProgramInfoDto.class);
        mDorPresenter.setSharedElementEnterTransition(getActivity(),
                DetailsActivity.SHARED_ELEMENT_NAME);

        setOnItemViewClickedListener(new ItemViewClickedListener());

    }

    @Override
    public void onResume() {
        super.onResume();
        TvApp.getApplication().getLogger().Debug("Resuming details fragment...");
        //reload with our updated item
        if (lastLoaded.before(TvApp.getApplication().getLastPlayback())) loadItem(mItemId);
    }

    private void loadItem(String id) {
        lastLoaded = Calendar.getInstance();
        mApplication.getApiClient().GetItemAsync(id, mApplication.getCurrentUser().getId(), new Response<BaseItemDto>() {
            @Override
            public void onResponse(BaseItemDto response) {
                mBaseItem = response;
                if (mChannelId != null) mBaseItem.setParentId(mChannelId);
                mDetailRowBuilderTask = (DetailRowBuilderTask) new DetailRowBuilderTask().execute(mBaseItem);
                updateBackground(Utils.getBackdropImageUrl(mBaseItem, TvApp.getApplication().getApiClient(), true));
            }

            @Override
            public void onError(Exception exception) {
                mApplication.getLogger().ErrorException("Error retrieving full object", exception);
                Utils.reportError(getActivity(), "Error retrieving item");
            }
        });

    }

    @Override
    public void onStop() {
        if (mDetailRowBuilderTask != null) mDetailRowBuilderTask.cancel(true);
        super.onStop();
    }

    protected void addItemRow(ArrayObjectAdapter parent, ItemRowAdapter row, int index, String headerText) {
        HeaderItem header = new HeaderItem(index, headerText, null);
        ListRow listRow = new ListRow(header, row);
        parent.add(listRow);
        row.setRow(listRow);
        row.Retrieve();
    }

    private class DetailRowBuilderTask extends AsyncTask<BaseItemDto, Integer, DetailsOverviewRow> {
        @Override
        protected DetailsOverviewRow doInBackground(BaseItemDto... baseItem) {


            DetailsOverviewRow row = new DetailsOverviewRow(mBaseItem);
            try {
                Bitmap poster = Picasso.with(getActivity())
                        .load(Utils.getPrimaryImageUrl(mBaseItem, mApiClient, true, false, Utils.convertDpToPixel(mApplication, DETAIL_THUMB_HEIGHT)))
                                .resize(Utils.convertDpToPixel(mApplication,DETAIL_THUMB_WIDTH),
                                        Utils.convertDpToPixel(mApplication, DETAIL_THUMB_HEIGHT))
                                .centerInside()
                                .get();
                row.setImageBitmap(getActivity(), poster);
            } catch (IOException e) {
            }

            UserItemDataDto userData = mBaseItem.getUserData();
            if (userData != null && userData.getPlaybackPositionTicks() > 0) {
                row.addAction(new Action(ACTION_RESUME, mActivity.getString(R.string.lbl_resume)));
            }

            switch (mBaseItem.getType()) {
                case "Person":
                case "Photo":
                    break;
                default:
                    if (mBaseItem.getIsFolder() && Utils.CanPlay(mBaseItem)) {
                        row.addAction(new Action(ACTION_PLAY, mActivity.getString(R.string.lbl_play_all)));
                        row.addAction(new Action(ACTION_SHUFFLE, mActivity.getString(R.string.lbl_shuffle_all)));

                    } else  {
                        if (Utils.CanPlay(mBaseItem)) row.addAction(new Action(ACTION_PLAY, mActivity.getString(R.string.lbl_play)));
                        if (mProgramInfo != null && TvApp.getApplication().getCurrentUser().getPolicy().getEnableLiveTvManagement()) {
                            //Add record buttons
                            if (mProgramInfo.getTimerId() != null) {
                                //existing recording
                                row.addAction(new Action(ACTION_CANCEL_RECORD, mActivity.getString(R.string.lbl_cancel)));
                            } else {
                                row.addAction(new Action(ACTION_RECORD, mActivity.getString(R.string.lbl_record)));
                            }
                        }
                        row.addAction(new Action(ACTION_DETAILS, mActivity.getString(R.string.lbl_details)));
                    }

            }
            return row;
        }

        protected void play(final BaseItemDto item, final int pos, final boolean shuffle) {
            Utils.getItemsToPlay(item, pos == 0 && item.getType().equals("Movie"), new Response<String[]>() {
                @Override
                public void onResponse(String[] response) {
                    Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);
                    if (shuffle) Collections.shuffle(Arrays.asList(response));
                    intent.putExtra("Items", response);
                    intent.putExtra("Position", pos);
                    startActivity(intent);
                }
            });

        }

        protected void play(final BaseItemDto[] items, final int pos, final boolean shuffle) {
            List<String> itemsToPlay = new ArrayList<>();
            final GsonJsonSerializer serializer = mApplication.getSerializer();

            for (BaseItemDto item : items) {
                itemsToPlay.add(serializer.SerializeToString(item));
            }

            Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);
            if (shuffle) Collections.shuffle(itemsToPlay);
            intent.putExtra("Items", itemsToPlay.toArray(new String[itemsToPlay.size()]));
            intent.putExtra("Position", pos);
            startActivity(intent);

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
                    Long id = action.getId();
                    switch (id.intValue()) {
                        case ACTION_PLAY:
                            play(mBaseItem, 0, false);
                            break;
                        case ACTION_RESUME:
                            Long pos = mBaseItem.getUserData().getPlaybackPositionTicks() / 10000;
                            play(mBaseItem, pos.intValue(), false);
                            break;
                        case ACTION_SHUFFLE:
                            play(mBaseItem, 0, true);
                            break;
                        case ACTION_RECORD:
                            recordProgram(mProgramInfo);
                            break;
                        case ACTION_CANCEL_RECORD:
                            cancelRecording(mProgramInfo);
                            break;
                        case ACTION_DETAILS:
                            Intent intent = new Intent(mActivity, FullDetailsActivity.class);
                            intent.putExtra("BaseItem", TvApp.getApplication().getSerializer().SerializeToString(mBaseItem));
                            mActivity.startActivity(intent);
                            break;
                        default:
                            Toast.makeText(getActivity(), action.toString() + mActivity.getString(R.string.msg_not_implemented), Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            });

            ps.addClassPresenter(DetailsOverviewRow.class, mDorPresenter);
            ps.addClassPresenter(ListRow.class,
                    new ListRowPresenter());

            final ArrayObjectAdapter adapter = new ArrayObjectAdapter(ps);
            adapter.add(detailRow);

            setAdapter(adapter);

            addAdditionalRows(adapter);
        }

    }

    protected void recordProgram(ProgramInfoDto program) {
        //Create timer with default settings
        TvApp.getApplication().getApiClient().GetDefaultLiveTvTimerInfo(program.getId(), new Response<SeriesTimerInfoDto>() {
            @Override
            public void onResponse(SeriesTimerInfoDto response) {
                TvApp.getApplication().getApiClient().CreateLiveTvTimerAsync(response, new EmptyResponse());
                Utils.showToast(mActivity, mActivity.getString(R.string.msg_set_to_record));
                //re-create details
                mProgramInfo.setTimerId(response.getId());
                mDetailRowBuilderTask = (DetailRowBuilderTask) new DetailRowBuilderTask().execute(mBaseItem);

            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error creating live tv recording", exception);
                Utils.showToast(mActivity, mActivity.getString(R.string.msg_unable_to_create_recording));
            }
        });
    }

    protected void cancelRecording(ProgramInfoDto program) {
        TvApp.getApplication().getApiClient().CancelLiveTvTimerAsync(program.getTimerId(), new EmptyResponse() {
            @Override
            public void onResponse() {
                Utils.showToast(mActivity, mActivity.getString(R.string.msg_recording_cancelled));
                mProgramInfo.setTimerId(null);
                mDetailRowBuilderTask = (DetailRowBuilderTask) new DetailRowBuilderTask().execute(mBaseItem);

            }
            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error cancelling live tv recording", exception);
                Utils.showToast(mActivity, mActivity.getString(R.string.msg_unable_to_cancel));
            }
        });
    }

    protected void addAdditionalRows(ArrayObjectAdapter adapter) {
        switch (mBaseItem.getType()) {
            case "Movie":

                //Cast/Crew
                if (mBaseItem.getPeople() != null) {
                    ItemRowAdapter castAdapter = new ItemRowAdapter(mBaseItem.getPeople(), new CardPresenter(), adapter);
                    addItemRow(adapter, castAdapter, 0, mActivity.getString(R.string.lbl_cast_crew));
                }

                //Chapters
                if (mBaseItem.getChapters() != null && mBaseItem.getChapters().size() > 0) {
                    List<ChapterItemInfo> chapters = new ArrayList<>();
                    ImageOptions options = new ImageOptions();
                    options.setImageType(ImageType.Chapter);
                    int i = 0;
                    for (ChapterInfoDto dto : mBaseItem.getChapters()) {
                        ChapterItemInfo chapter = new ChapterItemInfo();
                        chapter.setItemId(mBaseItem.getId());
                        chapter.setName(dto.getName());
                        chapter.setStartPositionTicks(dto.getStartPositionTicks());
                        if (dto.getHasImage()) {
                            options.setTag(dto.getImageTag());
                            options.setImageIndex(i);
                            chapter.setImagePath(mApiClient.GetImageUrl(mBaseItem.getId(), options));
                        }
                        chapters.add(chapter);
                        i++;
                    }

                    ItemRowAdapter chapterAdapter = new ItemRowAdapter(chapters, new CardPresenter(), adapter);
                    addItemRow(adapter, chapterAdapter, 1, mActivity.getString(R.string.lbl_chapters));
                }

                //Specials
                if (mBaseItem.getSpecialFeatureCount() != null && mBaseItem.getSpecialFeatureCount() > 0) {
                    addItemRow(adapter, new ItemRowAdapter(new SpecialsQuery(mBaseItem.getId()), new CardPresenter(), adapter), 2, mActivity.getString(R.string.lbl_specials));
                }

                //Trailers
                if (mBaseItem.getLocalTrailerCount() != null && mBaseItem.getLocalTrailerCount() > 0) {
                    addItemRow(adapter, new ItemRowAdapter(new TrailersQuery(mBaseItem.getId()), new CardPresenter(), adapter), 3, mActivity.getString(R.string.lbl_trailers));
                }

                //Similar
                SimilarItemsQuery similar = new SimilarItemsQuery();
                similar.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio});
                similar.setUserId(TvApp.getApplication().getCurrentUser().getId());
                similar.setId(mBaseItem.getId());
                similar.setLimit(10);

                ItemRowAdapter similarMoviesAdapter = new ItemRowAdapter(similar, QueryType.SimilarMovies, new CardPresenter(), adapter);
                addItemRow(adapter, similarMoviesAdapter, 4, mActivity.getString(R.string.lbl_similar_movies));
                break;
            case "Person":

                ItemQuery personMovies = new ItemQuery();
                personMovies.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
                personMovies.setUserId(TvApp.getApplication().getCurrentUser().getId());
                personMovies.setPerson(mBaseItem.getName());
                personMovies.setRecursive(true);
                personMovies.setIncludeItemTypes(new String[] {"Movie"});
                ItemRowAdapter personMoviesAdapter = new ItemRowAdapter(personMovies, 100, new CardPresenter(), adapter);
                addItemRow(adapter, personMoviesAdapter, 0, mApplication.getString(R.string.lbl_movies));

                ItemQuery personSeries = new ItemQuery();
                personSeries.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
                personSeries.setUserId(TvApp.getApplication().getCurrentUser().getId());
                personSeries.setPerson(mBaseItem.getName());
                personSeries.setRecursive(true);
                personSeries.setIncludeItemTypes(new String[] {"Series", "Episode"});
                ItemRowAdapter personSeriesAdapter = new ItemRowAdapter(personSeries, 100, new CardPresenter(), adapter);
                addItemRow(adapter, personSeriesAdapter, 1, mApplication.getString(R.string.lbl_tv_series));

                break;
            case "Series":
                NextUpQuery nextUpQuery = new NextUpQuery();
                nextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
                nextUpQuery.setSeriesId(mBaseItem.getId());
                nextUpQuery.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
                ItemRowAdapter nextUpAdapter = new ItemRowAdapter(nextUpQuery, false, new CardPresenter(), adapter);
                addItemRow(adapter, nextUpAdapter, 0, mApplication.getString(R.string.lbl_next_up));

                SeasonQuery seasons = new SeasonQuery();
                seasons.setSeriesId(mBaseItem.getId());
                seasons.setUserId(TvApp.getApplication().getCurrentUser().getId());
                ItemRowAdapter seasonsAdapter = new ItemRowAdapter(seasons, new CardPresenter(), adapter);
                addItemRow(adapter, seasonsAdapter, 1, mActivity.getString(R.string.lbl_seasons));

                UpcomingEpisodesQuery upcoming = new UpcomingEpisodesQuery();
                upcoming.setUserId(TvApp.getApplication().getCurrentUser().getId());
                upcoming.setParentId(mBaseItem.getId());
                upcoming.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
                ItemRowAdapter upcomingAdapter = new ItemRowAdapter(upcoming, new CardPresenter(), adapter);
                addItemRow(adapter, upcomingAdapter, 2, mActivity.getString(R.string.lbl_upcoming));

                if (mBaseItem.getPeople() != null) {
                    ItemRowAdapter seriesCastAdapter = new ItemRowAdapter(mBaseItem.getPeople(), new CardPresenter(), adapter);
                    addItemRow(adapter, seriesCastAdapter, 3, mApplication.getString(R.string.lbl_cast_crew));

                }

                SimilarItemsQuery similarSeries = new SimilarItemsQuery();
                similarSeries.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
                similarSeries.setUserId(TvApp.getApplication().getCurrentUser().getId());
                similarSeries.setId(mBaseItem.getId());
                similarSeries.setLimit(20);
                ItemRowAdapter similarAdapter = new ItemRowAdapter(similarSeries, QueryType.SimilarSeries, new CardPresenter(), adapter);
                addItemRow(adapter, similarAdapter, 4, mActivity.getString(R.string.lbl_similar_series));
                break;
        }


    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            ItemLauncher.launch((BaseRowItem) item, mApplication, getActivity(), itemViewHolder);
        }
    }

    protected void updateBackground(String url) {
        Picasso.with(getActivity())
                .load(url)
                .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                .error(mDefaultBackground)
                .into(mBackgroundTarget);
    }

}
