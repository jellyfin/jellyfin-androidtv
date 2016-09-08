package tv.emby.embyatv.browsing;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemLauncher;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.presentation.PositionableListRowPresenter;
import tv.emby.embyatv.querying.QueryType;
import tv.emby.embyatv.querying.ViewQuery;

/**
 * Created by Eric on 4/15/2015.
 */
public class CustomBrowseFragment extends Fragment implements IRowLoader {
    private RowsFragment mRowsFragment;
    private static final int BACKGROUND_UPDATE_DELAY = 100;

    protected String MainTitle;
    protected boolean ShowBadge = true;
    protected TvApp mApplication;
    protected BaseActivity mActivity;
    protected BaseRowItem mCurrentItem;
    protected ListRow mCurrentRow;
    protected CompositeClickedListener mClickedListener = new CompositeClickedListener();
    protected CompositeSelectedListener mSelectedListener = new CompositeSelectedListener();
    protected ArrayObjectAdapter mRowsAdapter;
    private Drawable mDefaultBackground;
    private SimpleTarget<Bitmap> mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private final Handler mHandler = new Handler();
    private String mBackgroundUrl;
    protected ArrayList<BrowseRowDef> mRows = new ArrayList<>();
    CardPresenter mCardPresenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_custom_browse, container, false);

        // Inject the RowsFragment in the results container
        if (getChildFragmentManager().findFragmentById(R.id.rows_area) == null) {
            mRowsFragment = new RowsFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.rows_area, mRowsFragment).commit();
        } else {
            mRowsFragment = (RowsFragment) getChildFragmentManager()
                    .findFragmentById(R.id.rows_area);
        }

        mRowsAdapter = new ArrayObjectAdapter(new PositionableListRowPresenter());
        mRowsFragment.setAdapter(mRowsAdapter);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mApplication = TvApp.getApplication();
        //mActivity = (BaseActivity) getActivity();

        prepareBackgroundManager();

        setupUIElements();

        setupQueries(this);

        setupEventListeners();
    }

    protected void setupQueries(IRowLoader rowLoader) {
        rowLoader.loadRows(mRows);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //Re-retrieve anything that needs it but delay slightly so we don't take away gui landing
        if (mRowsAdapter != null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < mRowsAdapter.size(); i++) {
                        if (mRowsAdapter.get(i) instanceof ListRow) {
                            if (((ListRow) mRowsAdapter.get(i)).getAdapter() instanceof ItemRowAdapter) {
                                ((ItemRowAdapter) ((ListRow) mRowsAdapter.get(i)).getAdapter()).ReRetrieveIfNeeded();
                            }
                        }
                    }
                }
            },1500);
        }
    }

    public void loadRows(List<BrowseRowDef> rows) {

        mRowsAdapter = new ArrayObjectAdapter(new PositionableListRowPresenter());
        mCardPresenter = new CardPresenter();

        for (BrowseRowDef def : rows) {
            HeaderItem header = new HeaderItem(def.getHeaderText());
            ItemRowAdapter rowAdapter;
            switch (def.getQueryType()) {
                case NextUp:
                    rowAdapter = new ItemRowAdapter(def.getNextUpQuery(), true, mCardPresenter, mRowsAdapter);
                    break;
                case Season:
                    rowAdapter = new ItemRowAdapter(def.getSeasonQuery(), mCardPresenter, mRowsAdapter);
                    break;
                case Upcoming:
                    rowAdapter = new ItemRowAdapter(def.getUpcomingQuery(), mCardPresenter, mRowsAdapter);
                    break;
                case Views:
                    rowAdapter = new ItemRowAdapter(new ViewQuery(), mCardPresenter, mRowsAdapter);
                    break;
                case SimilarSeries:
                    rowAdapter = new ItemRowAdapter(def.getSimilarQuery(), QueryType.SimilarSeries, mCardPresenter, mRowsAdapter);
                    break;
                case SimilarMovies:
                    rowAdapter = new ItemRowAdapter(def.getSimilarQuery(), QueryType.SimilarMovies, mCardPresenter, mRowsAdapter);
                    break;
                case Persons:
                    rowAdapter = new ItemRowAdapter(def.getPersonsQuery(), def.getChunkSize(), mCardPresenter, mRowsAdapter);
                    break;
                case LiveTvChannel:
                    rowAdapter = new ItemRowAdapter(def.getTvChannelQuery(), 40, mCardPresenter, mRowsAdapter);
                    break;
                case LiveTvProgram:
                    rowAdapter = new ItemRowAdapter(def.getProgramQuery(), mCardPresenter, mRowsAdapter);
                    break;
                case LiveTvRecording:
                    rowAdapter = new ItemRowAdapter(def.getRecordingQuery(), def.getChunkSize(), mCardPresenter, mRowsAdapter);
                    break;
                default:
                    rowAdapter = new ItemRowAdapter(def.getQuery(), def.getChunkSize(), def.getPreferParentThumb(), def.isStaticHeight(), mCardPresenter, mRowsAdapter);
                    break;
            }

            rowAdapter.setReRetrieveTriggers(def.getChangeTriggers());

            ListRow row = new ListRow(header, rowAdapter);
            mRowsAdapter.add(row);
            rowAdapter.setRow(row);
            rowAdapter.Retrieve();
        }

        addAdditionalRows(mRowsAdapter);

        mRowsFragment.setAdapter(mRowsAdapter);

    }

    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {

    }

    private void prepareBackgroundManager() {

        final BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.moviebg);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mBackgroundTarget = new SimpleTarget<Bitmap>(mMetrics.widthPixels, mMetrics.heightPixels) {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                backgroundManager.setBitmap(resource);
            }
        };

    }

    protected void setupUIElements() {
    }

    protected void setupEventListeners() {

        mRowsFragment.setOnItemViewClickedListener(mClickedListener);
        mClickedListener.registerListener(new ItemViewClickedListener());

        mRowsFragment.setOnItemViewSelectedListener(mSelectedListener);
        mSelectedListener.registerListener(new ItemViewSelectedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow)row).getAdapter(), ((BaseRowItem) item).getIndex(), getActivity());
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (!(item instanceof BaseRowItem)) {
                //fill in default background
                mBackgroundUrl = null;
                startBackgroundTimer();
                return;
            }

            BaseRowItem rowItem = (BaseRowItem) item;
            mCurrentItem = rowItem;
            mCurrentRow = (ListRow) row;

            //mApplication.getLogger().Debug("Selected Item "+rowItem.getIndex() + " type: "+ (rowItem.getItemType().equals(BaseRowItem.ItemType.BaseItem) ? rowItem.getBaseItem().getType() : "other"));
            ItemRowAdapter adapter = (ItemRowAdapter) ((ListRow)row).getAdapter();
            adapter.loadMoreItemsIfNeeded(rowItem.getIndex());

            mBackgroundUrl = rowItem.getBackdropImageUrl();
            startBackgroundTimer();

        }
    }

    protected void setDefaultBackground(Drawable background) {
        mDefaultBackground = background;
    }

    protected void setDefaultBackground(int resourceId) {
        mDefaultBackground = getResources().getDrawable(resourceId);
    }

    protected void updateBackground(String url) {
        Glide.with(getActivity())
                .load(url)
                .asBitmap()
                .override(mMetrics.widthPixels, mMetrics.heightPixels)
                .centerCrop()
                .error(mDefaultBackground)
                .into(mBackgroundTarget);
    }

    protected void updateBackground(Drawable drawable) {
        BackgroundManager.getInstance(getActivity()).setDrawable(drawable);
    }

    protected void clearBackground() {
        BackgroundManager.getInstance(getActivity()).setDrawable(mDefaultBackground);
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBackgroundUrl != null) {
                        updateBackground(mBackgroundUrl);
                    } else {
                        updateBackground(mDefaultBackground);
                    }
                }
            });

        }
    }


}
