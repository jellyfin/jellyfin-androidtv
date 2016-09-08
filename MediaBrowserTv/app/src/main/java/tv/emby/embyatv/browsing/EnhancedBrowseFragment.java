package tv.emby.embyatv.browsing;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
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
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.DisplayPreferences;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.CustomMessage;
import tv.emby.embyatv.base.IKeyListener;
import tv.emby.embyatv.base.IMessageListener;
import tv.emby.embyatv.details.ItemListActivity;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemLauncher;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.presentation.GridButtonPresenter;
import tv.emby.embyatv.presentation.PositionableListRowPresenter;
import tv.emby.embyatv.querying.QueryType;
import tv.emby.embyatv.querying.ViewQuery;
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
    protected static final int GRID = 6;
    protected static final int ALBUMS = 7;
    protected static final int ARTISTS = 8;
    public static final int FAVSONGS = 9;
    protected BaseItemDto mFolder;
    protected String itemTypeString;
    protected boolean showViews = true;
    protected boolean justLoaded = true;
    protected boolean ShowFanart = false;

    protected BaseRowItem favSongsRowItem;

    private SimpleTarget<Bitmap> mBackgroundTarget;
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
    protected ListRow mCurrentRow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BaseItemDto item = new BaseItemDto();
        item.setId(ItemListActivity.FAV_SONGS);
        item.setType("Playlist");
        item.setIsFolder(true);

        favSongsRowItem = new BaseRowItem(0, item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.enhanced_detail_browse, container, false);

        mActivity = (BaseActivity) getActivity();
        roboto = TvApp.getApplication().getDefaultFont();

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

        mRowsAdapter = new ArrayObjectAdapter(new PositionableListRowPresenter());
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
        if (mFolder == null) return;

        if (mFolder.getCollectionType() != null) {
            switch (mFolder.getCollectionType()) {
                case "movies":
                    itemTypeString = "Movie";
                    break;
                case "tvshows":
                    itemTypeString = "Series";
                    break;
                case "music":
                    itemTypeString = "MusicAlbum";
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

        ShowFanart = mApplication.getPrefs().getBoolean("pref_show_backdrop", true);

        //React to deletion
        if (getActivity() != null && !getActivity().isFinishing() && mCurrentRow != null && mCurrentItem != null && mCurrentItem.getItemId() != null && mCurrentItem.getItemId().equals(TvApp.getApplication().getLastDeletedItemId())) {
            ((ItemRowAdapter)mCurrentRow.getAdapter()).remove(mCurrentItem);
            TvApp.getApplication().setLastDeletedItemId(null);
        }

        if (!justLoaded) {
            //Re-retrieve anything that needs it but delay slightly so we don't take away gui landing
            if (mRowsAdapter != null) {
                refreshCurrentItem();
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
                }, 1500);
            }

        } else {
            justLoaded = false;
        }
    }

    public void loadRows(List<BrowseRowDef> rows) {

        mRowsAdapter = new ArrayObjectAdapter(new PositionableListRowPresenter());
        mCardPresenter = new CardPresenter(false, 280);
        ClassPresenterSelector ps = new ClassPresenterSelector();
        ps.addClassPresenter(BaseRowItem.class, mCardPresenter);
        ps.addClassPresenter(GridButton.class, new GridButtonPresenter(false, 310, 280));


        for (BrowseRowDef def : rows) {
            HeaderItem header = new HeaderItem(def.getHeaderText());
            ItemRowAdapter rowAdapter;
            switch (def.getQueryType()) {
                case NextUp:
                    rowAdapter = new ItemRowAdapter(def.getNextUpQuery(), true, mCardPresenter, mRowsAdapter);
                    break;
                case LatestItems:
                    rowAdapter = new ItemRowAdapter(def.getLatestItemsQuery(), true, mCardPresenter, mRowsAdapter);
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
                case LiveTvRecordingGroup:
                    rowAdapter = new ItemRowAdapter(def.getRecordingGroupQuery(), mCardPresenter, mRowsAdapter);
                    break;
                case Premieres:
                    rowAdapter = new ItemRowAdapter(def.getQuery(), def.getChunkSize(), def.getPreferParentThumb(), def.isStaticHeight(), mCardPresenter, mRowsAdapter, def.getQueryType());
                    break;
                default:
                    rowAdapter = new ItemRowAdapter(def.getQuery(), def.getChunkSize(), def.getPreferParentThumb(), def.isStaticHeight(), ps, mRowsAdapter, def.getQueryType());
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
            HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), mApplication.getString(R.string.lbl_views));

            GridButtonPresenter mGridPresenter = new GridButtonPresenter();
            ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
            switch (itemTypeString) {
                case "Movie":
                    gridRowAdapter.add(new GridButton(SUGGESTED, mApplication.getString(R.string.lbl_suggested), R.drawable.suggestions));
                    addStandardViewButtons(gridRowAdapter);
                    break;
                case "MusicAlbum":
                    gridRowAdapter.add(new GridButton(ALBUMS, TvApp.getApplication().getString(R.string.lbl_albums), R.drawable.audio));
                    gridRowAdapter.add(new GridButton(ARTISTS, TvApp.getApplication().getString(R.string.lbl_artists), R.drawable.artists));
                    gridRowAdapter.add(new GridButton(GENRES, mApplication.getString(R.string.lbl_genres), R.drawable.genres));
                    gridRowAdapter.add(new GridButton(SEARCH, mApplication.getString(R.string.lbl_search), R.drawable.search));
                    break;
                default:
                    addStandardViewButtons(gridRowAdapter);
                    break;
            }
            rowAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        }

    }

    protected void addStandardViewButtons(ArrayObjectAdapter gridRowAdapter) {
        gridRowAdapter.add(new GridButton(GRID, TvApp.getApplication().getString(R.string.lbl_all_items), R.drawable.grid));
        gridRowAdapter.add(new GridButton(BY_LETTER, mApplication.getString(R.string.lbl_by_letter), R.drawable.byletter));
        gridRowAdapter.add(new GridButton(GENRES, mApplication.getString(R.string.lbl_genres), R.drawable.genres));
        //gridRowAdapter.add(new GridButton(PERSONS, mApplication.getString(R.string.lbl_performers), R.drawable.actors));
        gridRowAdapter.add(new GridButton(SEARCH, mApplication.getString(R.string.lbl_search), R.drawable.search));

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
                            refreshCurrentItem();
                            break;
                    }
                }
            });
        }
    }

    private void refreshCurrentItem() {
        if (mCurrentItem != null && !"Photo".equals(mCurrentItem.getType()) && !"MusicArtist".equals(mCurrentItem.getType())
                && !"MusicAlbum".equals(mCurrentItem.getType()) && !"Playlist".equals(mCurrentItem.getType())) {
            TvApp.getApplication().getLogger().Debug("Refresh item "+mCurrentItem.getFullName());
            mCurrentItem.refresh(new EmptyResponse() {
                @Override
                public void onResponse() {
                    ItemRowAdapter adapter = (ItemRowAdapter) ((ListRow) mCurrentRow).getAdapter();
                    adapter.notifyArrayItemRangeChanged(adapter.indexOf(mCurrentItem), 1);
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
                    case GRID:
                        TvApp.getApplication().getDisplayPrefsAsync(mFolder.getDisplayPreferencesId(), new Response<DisplayPreferences>() {
                            @Override
                            public void onResponse(DisplayPreferences response) {
                                Intent folderIntent = new Intent(getActivity(), GenericGridActivity.class);
                                folderIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(mFolder));
                                getActivity().startActivity(folderIntent);
                            }
                        });
                        break;

                    case ALBUMS:
                        mFolder.setDisplayPreferencesId(mFolder.getId()+"AL");
                        TvApp.getApplication().getDisplayPrefsAsync(mFolder.getDisplayPreferencesId(), new Response<DisplayPreferences>() {
                            @Override
                            public void onResponse(DisplayPreferences response) {
                                Intent folderIntent = new Intent(getActivity(), GenericGridActivity.class);
                                folderIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(mFolder));
                                folderIntent.putExtra("IncludeType", "MusicAlbum");
                                getActivity().startActivity(folderIntent);
                            }
                        });
                        break;

                    case ARTISTS:
                        mFolder.setDisplayPreferencesId(mFolder.getId()+"AR");
                        TvApp.getApplication().getDisplayPrefsAsync(mFolder.getDisplayPreferencesId(), new Response<DisplayPreferences>() {
                            @Override
                            public void onResponse(DisplayPreferences response) {
                                Intent folderIntent = new Intent(getActivity(), GenericGridActivity.class);
                                folderIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(mFolder));
                                folderIntent.putExtra("IncludeType", "AlbumArtist");
                                getActivity().startActivity(folderIntent);
                            }
                        });
                        break;

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
                        TvApp.getApplication().showSearch(getActivity(), "MusicAlbum".equals(itemTypeString));
                        break;

                    case FAVSONGS:
                        Intent favIntent = new Intent(getActivity(), ItemListActivity.class);
                        favIntent.putExtra("ItemId", ItemListActivity.FAV_SONGS);
                        favIntent.putExtra("ParentId", mFolder.getId());

                        getActivity().startActivity(favIntent);
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
            MediaManager.setCurrentMediaTitle(row.getHeaderItem().getName());
            ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow)row).getAdapter(), ((BaseRowItem)item).getIndex(), getActivity());
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            mHandler.removeCallbacks(updateContentTask);
            if (item instanceof GridButton && ((GridButton)item).getId() == FAVSONGS) {
                //set to specialized item
                mCurrentItem = favSongsRowItem;
            }

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
            mCurrentRow = (ListRow) row;
            mTitle.setText(mCurrentItem.getName());
            mInfoRow.removeAllViews();
            mSummary.setText("");
            mHandler.postDelayed(updateContentTask, 500);

            //mApplication.getLogger().Debug("Selected Item "+rowItem.getIndex() + " type: "+ (rowItem.getItemType().equals(BaseRowItem.ItemType.BaseItem) ? rowItem.getBaseItem().getType() : "other"));
            ItemRowAdapter adapter = (ItemRowAdapter) ((ListRow)row).getAdapter();
            adapter.loadMoreItemsIfNeeded(rowItem.getIndex());

            if (ShowFanart) {
                mBackgroundUrl = rowItem.getBackdropImageUrl();
                startBackgroundTimer();
            }

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

    protected void updateBackground(String url) {
        if (url == null) {
            clearBackground();
        } else {
            Glide.with(getActivity())
                    .load(url)
                    .asBitmap()
                    .override(mMetrics.widthPixels, mMetrics.heightPixels)
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
