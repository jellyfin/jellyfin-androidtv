package tv.emby.embyatv.browsing;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.CustomMessage;
import tv.emby.embyatv.base.IKeyListener;
import tv.emby.embyatv.base.IMessageListener;
import tv.emby.embyatv.imagehandling.PicassoBackgroundManagerTarget;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemLauncher;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.presentation.GridButtonPresenter;
import tv.emby.embyatv.querying.QueryType;
import tv.emby.embyatv.querying.ViewQuery;
import tv.emby.embyatv.search.SearchActivity;
import tv.emby.embyatv.ui.GridButton;
import tv.emby.embyatv.util.InfoLayoutHelper;
import tv.emby.embyatv.util.KeyProcessor;

/**
 * Created by Eric on 5/17/2015.
 */
public class EnhancedBrowseFragment extends Fragment implements IRowLoader {
    private static final int BACKGROUND_UPDATE_DELAY = 100;
    Typeface roboto;
    BaseActivity mActivity;
    TvApp mApplication;

    TextView mTitle;
    LinearLayout mInfoRow;
    TextView mSummary;

    protected static final int BY_LETTER = 0;
    protected static final int GENRES = 1;
    protected static final int YEARS = 2;
    protected static final int PERSONS = 3;
    protected static final int SUGGESTED = 4;
    protected static final int SEARCH = 5;
    protected BaseItemDto mFolder;
    protected String itemTypeString;
    protected boolean showViews = true;

    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;

    RowsFragment mRowsFragment;
    protected CompositeClickedListener mClickedListener = new CompositeClickedListener();
    protected CompositeSelectedListener mSelectedListener = new CompositeSelectedListener();
    protected ArrayObjectAdapter mRowsAdapter;
    private Drawable mDefaultBackground;
    private Timer mBackgroundTimer;
    private final Handler mHandler = new Handler();
    private String mBackgroundUrl;
    protected ArrayList<BrowseRowDef> mRows = new ArrayList<>();
    CardPresenter mCardPresenter;
    protected BaseRowItem mCurrentItem;
    protected Row mCurrentRow;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.enhanced_detail_browse, container, false);

        mActivity = (BaseActivity) getActivity();
        roboto = Typeface.createFromAsset(mActivity.getAssets(), "fonts/Roboto-Light.ttf");

        mTitle = (TextView) root.findViewById(R.id.title);
        mTitle.setTypeface(roboto);
        mTitle.setShadowLayer(5, 5, 5, Color.BLACK);
        mInfoRow = (LinearLayout) root.findViewById(R.id.infoRow);
        mSummary = (TextView) root.findViewById(R.id.summary);
        mSummary.setTypeface(roboto);
        mSummary.setShadowLayer(5, 5, 5, Color.BLACK);

        // Inject the RowsFragment in the results container
        if (getChildFragmentManager().findFragmentById(R.id.rowsFragment) == null) {
            mRowsFragment = new RowsFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.rowsFragment, mRowsFragment).commit();
        } else {
            mRowsFragment = (RowsFragment) getChildFragmentManager()
                    .findFragmentById(R.id.rowsFragment);
        }

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mRowsFragment.setAdapter(mRowsAdapter);

        return root;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mApplication = TvApp.getApplication();
        mDefaultBackground = mApplication.getDrawableCompat(R.drawable.blank10x10);

        prepareBackgroundManager();

        setupViews();

        setupUIElements();

        setupQueries(this);

        setupEventListeners();
    }

    protected void setupQueries(IRowLoader rowLoader) {
        rowLoader.loadRows(mRows);
    }

    protected void setupViews() {
        mFolder = TvApp.getApplication().getSerializer().DeserializeFromString(getActivity().getIntent().getStringExtra("Folder"),BaseItemDto.class);
        if (mFolder.getCollectionType() != null) {
            switch (mFolder.getCollectionType()) {
                case "movies":
                    itemTypeString = "Movie";
                    break;
                case "tvshows":
                    itemTypeString = "Series";
                    break;
                case "folders":
                    showViews = false;
                    break;
                default:
                    showViews = false;
            }
        } else {
            showViews = false;
        }

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
                    if (mActivity.isFinishing()) return;
                    for (int i = 0; i < mRowsAdapter.size(); i++) {
                        if (mRowsAdapter.get(i) instanceof ListRow) {
                            if (((ListRow) mRowsAdapter.get(i)).getAdapter() instanceof ItemRowAdapter && !mActivity.isFinishing()) {
                                ((ItemRowAdapter) ((ListRow) mRowsAdapter.get(i)).getAdapter()).ReRetrieveIfNeeded();
                            }
                        }
                    }
                }
            },1500);
        }
    }

    public void loadRows(List<BrowseRowDef> rows) {

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mCardPresenter = new CardPresenter(false);

        for (BrowseRowDef def : rows) {
            HeaderItem header = new HeaderItem(def.getHeaderText(), null);
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
                    rowAdapter = new ItemRowAdapter(def.getRecordingQuery(), mCardPresenter, mRowsAdapter);
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
        if (showViews) {
            HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), mApplication.getString(R.string.lbl_views), null);

            GridButtonPresenter mGridPresenter = new GridButtonPresenter();
            ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
            gridRowAdapter.add(new GridButton(BY_LETTER, mApplication.getString(R.string.lbl_by_letter), R.drawable.byletter));
            if (itemTypeString != null && itemTypeString.equals("Movie"))
                gridRowAdapter.add(new GridButton(SUGGESTED, mApplication.getString(R.string.lbl_suggested), R.drawable.suggestions));
            gridRowAdapter.add(new GridButton(GENRES, mApplication.getString(R.string.lbl_genres), R.drawable.genres));
            gridRowAdapter.add(new GridButton(PERSONS, mApplication.getString(R.string.lbl_performers), R.drawable.actors));
            gridRowAdapter.add(new GridButton(SEARCH, mApplication.getString(R.string.lbl_search), R.drawable.search));
            rowAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        }

    }

    protected void setupUIElements() {
    }

    protected void setupEventListeners() {

        mRowsFragment.setOnItemViewClickedListener(mClickedListener);
        mClickedListener.registerListener(new ItemViewClickedListener());
        if (showViews) mClickedListener.registerListener(new SpecialViewClickedListener());

        mRowsFragment.setOnItemViewSelectedListener(mSelectedListener);
        mSelectedListener.registerListener(new ItemViewSelectedListener());
        if (mActivity != null) {
            mActivity.registerKeyListener(new IKeyListener() {
                @Override
                public boolean onKeyUp(int key, KeyEvent event) {
                    return KeyProcessor.HandleKey(key, mCurrentItem, mActivity);
                }
            });

            mActivity.registerMessageListener(new IMessageListener() {
                @Override
                public void onMessageReceived(CustomMessage message) {
                    switch (message) {

                        case RefreshCurrentItem:
                            TvApp.getApplication().getLogger().Debug("Refresh item "+mCurrentItem.getFullName());
                            mCurrentItem.refresh(new EmptyResponse() {
                                @Override
                                public void onResponse() {
                                    ItemRowAdapter adapter = (ItemRowAdapter) ((ListRow)mCurrentRow).getAdapter();
                                    adapter.notifyArrayItemRangeChanged(adapter.indexOf(mCurrentItem), 1);
                                }
                            });
                            break;
                    }
                }
            });
        }
    }

    private final class SpecialViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof GridButton) {
                switch (((GridButton) item).getId()) {
                    case BY_LETTER:
                        Intent intent = new Intent(getActivity(), ByLetterActivity.class);
                        intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(mFolder));
                        intent.putExtra("IncludeType", itemTypeString);

                        getActivity().startActivity(intent);
                        break;

                    case GENRES:
                        Intent genreIntent = new Intent(getActivity(), ByGenreActivity.class);
                        genreIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(mFolder));
                        genreIntent.putExtra("IncludeType", itemTypeString);

                        getActivity().startActivity(genreIntent);
                        break;

                    case SUGGESTED:
                        Intent suggIntent = new Intent(getActivity(), SuggestedMoviesActivity.class);
                        suggIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(mFolder));
                        suggIntent.putExtra("IncludeType", itemTypeString);

                        getActivity().startActivity(suggIntent);
                        break;

                    case PERSONS:
                        Intent personIntent = new Intent(getActivity(), BrowsePersonsActivity.class);
                        personIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(mFolder));
                        personIntent.putExtra("IncludeType", itemTypeString);

                        getActivity().startActivity(personIntent);
                        break;

                    case SEARCH:
                        Intent searchIntent = new Intent(getActivity(), SearchActivity.class);
                        getActivity().startActivity(searchIntent);
                        break;

                    default:
                        Toast.makeText(getActivity(), item.toString() + mApplication.getString(R.string.msg_not_implemented), Toast.LENGTH_SHORT)
                                .show();
                        break;
                }
            }
        }
    }
    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            ItemLauncher.launch((BaseRowItem) item, mApplication, getActivity());
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            mHandler.removeCallbacks(updateContentTask);
            if (!(item instanceof BaseRowItem)) {
                mTitle.setText(mFolder.getName());
                mInfoRow.removeAllViews();
                mSummary.setText("");
                //fill in default background
                mBackgroundUrl = null;
                startBackgroundTimer();
                return;
            }

            BaseRowItem rowItem = (BaseRowItem) item;

            mCurrentItem = rowItem;
            mCurrentRow = row;
            mHandler.postDelayed(updateContentTask, 500);

            //mApplication.getLogger().Debug("Selected Item "+rowItem.getIndex() + " type: "+ (rowItem.getItemType().equals(BaseRowItem.ItemType.BaseItem) ? rowItem.getBaseItem().getType() : "other"));
            ItemRowAdapter adapter = (ItemRowAdapter) ((ListRow)row).getAdapter();
            adapter.loadMoreItemsIfNeeded(rowItem.getIndex());

            mBackgroundUrl = rowItem.getBackdropImageUrl();
            startBackgroundTimer();

        }
    }

    protected Runnable updateContentTask = new Runnable() {
        @Override
        public void run() {
            if (mCurrentItem == null) return;
            mTitle.setText(mCurrentItem.getName());
            mSummary.setText(mCurrentItem.getSummary());
            InfoLayoutHelper.addInfoRow(mActivity, mCurrentItem, mInfoRow, true, true);

        }
    };

    private void prepareBackgroundManager() {

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(R.drawable.moviebg);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    protected void updateBackground(String url) {
        if (url == null) {
            clearBackground();
        } else {
            Picasso.with(getActivity())
                    .load(url)
                    //.skipMemoryCache()
                    .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                    .centerCrop()
                    .error(mDefaultBackground)
                    .into(mBackgroundTarget);
        }
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
