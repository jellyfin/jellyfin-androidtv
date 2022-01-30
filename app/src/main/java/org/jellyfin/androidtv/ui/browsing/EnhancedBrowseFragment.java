package org.jellyfin.androidtv.ui.browsing;

import static org.koin.java.KoinJavaComponent.inject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.constant.Extras;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.querying.ViewQuery;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.databinding.EnhancedDetailBrowseBinding;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.ui.itemdetail.ItemListActivity;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuideActivity;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.ui.shared.BaseActivity;
import org.jellyfin.androidtv.ui.shared.KeyListener;
import org.jellyfin.androidtv.ui.shared.MessageListener;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.androidtv.util.MarkdownRenderer;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.serialization.GsonJsonSerializer;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.List;

import kotlin.Lazy;

public class EnhancedBrowseFragment extends Fragment implements RowLoader {
    protected FragmentActivity mActivity;

    protected TextView mTitle;
    private LinearLayout mInfoRow;
    private TextView mSummary;

    protected static final int BY_LETTER = 0;
    protected static final int GENRES = 1;
    protected static final int SUGGESTED = 4;
    protected static final int GRID = 6;
    protected static final int ALBUMS = 7;
    protected static final int ARTISTS = 8;
    public static final int FAVSONGS = 9;
    protected static final int SCHEDULE = 10;
    protected static final int SERIES = 11;
    protected BaseItemDto mFolder;
    protected String itemTypeString;
    protected boolean showViews = true;
    protected boolean justLoaded = true;

    protected BaseRowItem favSongsRowItem;

    protected RowsSupportFragment mRowsFragment;
    protected CompositeClickedListener mClickedListener = new CompositeClickedListener();
    protected CompositeSelectedListener mSelectedListener = new CompositeSelectedListener();
    protected ArrayObjectAdapter mRowsAdapter;
    protected ArrayList<BrowseRowDef> mRows = new ArrayList<>();
    protected CardPresenter mCardPresenter;
    protected BaseRowItem mCurrentItem;
    protected ListRow mCurrentRow;

    private Lazy<GsonJsonSerializer> serializer = inject(GsonJsonSerializer.class);
    private Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    private Lazy<MarkdownRenderer> markdownRenderer = inject(MarkdownRenderer.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BaseItemDto item = new BaseItemDto();
        item.setId(ItemListActivity.FAV_SONGS);
        item.setBaseItemType(BaseItemType.Playlist);
        item.setIsFolder(true);

        favSongsRowItem = new BaseRowItem(0, item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        EnhancedDetailBrowseBinding binding = EnhancedDetailBrowseBinding.inflate(inflater, container, false);

        mActivity = getActivity();

        mTitle = binding.title;
        mInfoRow = binding.infoRow;
        mSummary = binding.summary;

        // Inject the RowsSupportFragment in the results container
        if (getChildFragmentManager().findFragmentById(R.id.rowsFragment) == null) {
            mRowsFragment = new RowsSupportFragment();
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.rowsFragment, mRowsFragment).commit();
        } else {
            mRowsFragment = (RowsSupportFragment) getChildFragmentManager()
                    .findFragmentById(R.id.rowsFragment);
        }

        mRowsAdapter = new ArrayObjectAdapter(new PositionableListRowPresenter());
        mRowsFragment.setAdapter(mRowsAdapter);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        backgroundService.getValue().attach(requireActivity());

        setupViews();
        setupQueries(this);
        setupEventListeners();
    }

    protected void setupQueries(RowLoader rowLoader) {
        rowLoader.loadRows(mRows);
    }

    protected void setupViews() {
        mFolder = serializer.getValue().DeserializeFromString(requireActivity().getIntent().getStringExtra(Extras.Folder), BaseItemDto.class);
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
    public void onResume() {
        super.onResume();

        // React to deletion
        DataRefreshService dataRefreshService = KoinJavaComponent.<DataRefreshService>get(DataRefreshService.class);
        if (getActivity() != null && !getActivity().isFinishing() && mCurrentRow != null && mCurrentItem != null && mCurrentItem.getItemId() != null && mCurrentItem.getItemId().equals(dataRefreshService.getLastDeletedItemId())) {
            ((ItemRowAdapter) mCurrentRow.getAdapter()).remove(mCurrentItem);
            dataRefreshService.setLastDeletedItemId(null);
        }

        if (!justLoaded) {
            // Re-retrieve anything that needs it but delay slightly so we don't take away gui landing
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
                case SeriesTimer:
                    rowAdapter = new ItemRowAdapter(def.getSeriesTimerQuery(), mCardPresenter, mRowsAdapter);
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
        if (!showViews) return;

        HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), getString(R.string.lbl_views));
        GridButtonPresenter mGridPresenter = new GridButtonPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);

        switch (itemTypeString) {
            case "Movie":
                gridRowAdapter.add(new GridButton(SUGGESTED, getString(R.string.lbl_suggested), R.drawable.tile_suggestions, null));
                addStandardViewButtons(gridRowAdapter);
                break;

            case "MusicAlbum":
                gridRowAdapter.add(new GridButton(ALBUMS, getString(R.string.lbl_albums), R.drawable.tile_audio, null));
                gridRowAdapter.add(new GridButton(ARTISTS, getString(R.string.lbl_artists), R.drawable.tile_artists, null));
                gridRowAdapter.add(new GridButton(GENRES, getString(R.string.lbl_genres), R.drawable.tile_genres, null));
                break;

            default:
                addStandardViewButtons(gridRowAdapter);
                break;
        }

        rowAdapter.add(new ListRow(gridHeader, gridRowAdapter));
    }

    protected void addStandardViewButtons(ArrayObjectAdapter gridRowAdapter) {
        gridRowAdapter.add(new GridButton(GRID, getString(R.string.lbl_all_items), R.drawable.tile_port_grid, null));
        gridRowAdapter.add(new GridButton(BY_LETTER, getString(R.string.lbl_by_letter), R.drawable.tile_letters, null));
        gridRowAdapter.add(new GridButton(GENRES, getString(R.string.lbl_genres), R.drawable.tile_genres, null));
        // Disabled because the screen doesn't behave properly
        // gridRowAdapter.add(new GridButton(PERSONS, getString(R.string.lbl_performers), R.drawable.tile_actors));
    }

    protected void setupEventListeners() {
        mRowsFragment.setOnItemViewClickedListener(mClickedListener);
        mClickedListener.registerListener(new ItemViewClickedListener());
        if (showViews) mClickedListener.registerListener(new SpecialViewClickedListener());

        mRowsFragment.setOnItemViewSelectedListener(mSelectedListener);
        mSelectedListener.registerListener(new ItemViewSelectedListener());
        if (mActivity != null && mActivity instanceof BaseActivity) {
            ((BaseActivity) mActivity).registerKeyListener(new KeyListener() {
                @Override
                public boolean onKeyUp(int key, KeyEvent event) {
                    return KeyProcessor.HandleKey(key, mCurrentItem, ((BaseActivity) mActivity));
                }
            });

            ((BaseActivity) mActivity).registerMessageListener(new MessageListener() {
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
        if (mCurrentItem != null &&
                mCurrentItem.getBaseItemType() != BaseItemType.Photo &&
                mCurrentItem.getBaseItemType() != BaseItemType.MusicArtist &&
                mCurrentItem.getBaseItemType() != BaseItemType.MusicAlbum &&
                mCurrentItem.getBaseItemType() != BaseItemType.Playlist
        ) {
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
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof GridButton) {
                switch (((GridButton) item).getId()) {
                    case GRID:
                        Intent folderIntent = new Intent(getActivity(), GenericGridActivity.class);
                        folderIntent.putExtra(Extras.Folder, serializer.getValue().SerializeToString(mFolder));
                        requireActivity().startActivity(folderIntent);
                        break;

                    case ALBUMS:
                        mFolder.setDisplayPreferencesId(mFolder.getId() + "AL");

                        Intent albumsIntent = new Intent(getActivity(), GenericGridActivity.class);
                        albumsIntent.putExtra(Extras.Folder, serializer.getValue().SerializeToString(mFolder));
                        albumsIntent.putExtra(Extras.IncludeType, "MusicAlbum");
                        requireActivity().startActivity(albumsIntent);
                        break;

                    case ARTISTS:
                        mFolder.setDisplayPreferencesId(mFolder.getId() + "AR");

                        Intent artistsIntent = new Intent(getActivity(), GenericGridActivity.class);
                        artistsIntent.putExtra(GroupedItemsActivity.EXTRA_FOLDER, serializer.getValue().SerializeToString(mFolder));
                        artistsIntent.putExtra(GroupedItemsActivity.EXTRA_INCLUDE_TYPE, "AlbumArtist");
                        requireActivity().startActivity(artistsIntent);
                        break;

                    case BY_LETTER:
                        Intent letterIntent = new Intent(getActivity(), GroupedItemsActivity.class);
                        letterIntent.putExtra(GroupedItemsActivity.EXTRA_GROUPING_TYPE, GroupedItemsActivity.GroupingType.LETTER.toString());
                        letterIntent.putExtra(GroupedItemsActivity.EXTRA_FOLDER, serializer.getValue().SerializeToString(mFolder));
                        letterIntent.putExtra(GroupedItemsActivity.EXTRA_INCLUDE_TYPE, itemTypeString);

                        requireActivity().startActivity(letterIntent);
                        break;

                    case GENRES:
                        Intent genreIntent = new Intent(getActivity(), GroupedItemsActivity.class);
                        genreIntent.putExtra(GroupedItemsActivity.EXTRA_GROUPING_TYPE, GroupedItemsActivity.GroupingType.GENRE.toString());
                        genreIntent.putExtra(GroupedItemsActivity.EXTRA_FOLDER, serializer.getValue().SerializeToString(mFolder));
                        genreIntent.putExtra(GroupedItemsActivity.EXTRA_INCLUDE_TYPE, itemTypeString);

                        requireActivity().startActivity(genreIntent);
                        break;

                    case SUGGESTED:
                        Intent suggIntent = new Intent(getActivity(), SuggestedMoviesActivity.class);
                        suggIntent.putExtra(Extras.Folder, serializer.getValue().SerializeToString(mFolder));
                        suggIntent.putExtra(Extras.IncludeType, itemTypeString);

                        requireActivity().startActivity(suggIntent);
                        break;

                    case FAVSONGS:
                        Intent favIntent = new Intent(getActivity(), ItemListActivity.class);
                        favIntent.putExtra("ItemId", ItemListActivity.FAV_SONGS);
                        favIntent.putExtra("ParentId", mFolder.getId());

                        requireActivity().startActivity(favIntent);
                        break;

                    case SERIES:
                    case TvApp.LIVE_TV_SERIES_OPTION_ID:
                        Intent seriesIntent = new Intent(mActivity, UserViewActivity.class);
                        BaseItemDto seriesTimers = new BaseItemDto();
                        seriesTimers.setId("SERIESTIMERS");
                        seriesTimers.setCollectionType("SeriesTimers");
                        seriesTimers.setName(mActivity.getString(R.string.lbl_series_recordings));
                        seriesIntent.putExtra(Extras.Folder, serializer.getValue().SerializeToString(seriesTimers));

                        requireActivity().startActivity(seriesIntent);
                        break;

                    case SCHEDULE:
                    case TvApp.LIVE_TV_SCHEDULE_OPTION_ID:
                        Intent schedIntent = new Intent(mActivity, BrowseScheduleActivity.class);
                        requireActivity().startActivity(schedIntent);
                        break;

                    case TvApp.LIVE_TV_RECORDINGS_OPTION_ID:
                        Intent recordings = new Intent(mActivity, BrowseRecordingsActivity.class);
                        BaseItemDto folder = new BaseItemDto();
                        folder.setId("");
                        folder.setName(getString(R.string.lbl_recorded_tv));
                        recordings.putExtra(Extras.Folder, serializer.getValue().SerializeToString(folder));
                        mActivity.startActivity(recordings);
                        break;

                    case TvApp.LIVE_TV_GUIDE_OPTION_ID:
                        Intent guide = new Intent(mActivity, LiveTvGuideActivity.class);
                        mActivity.startActivity(guide);
                        break;

                    default:
                        Toast.makeText(getActivity(), item.toString() + getString(R.string.msg_not_implemented), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (!(item instanceof BaseRowItem)) return;

            mediaManager.getValue().setCurrentMediaTitle(row.getHeaderItem().getName());
            ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow) row).getAdapter(), ((BaseRowItem) item).getIndex(), getActivity());
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof GridButton && ((GridButton) item).getId() == FAVSONGS) {
                // Set to specialized item
                mCurrentItem = favSongsRowItem;
            }

            if (!(item instanceof BaseRowItem)) {
                mTitle.setText(mFolder != null ? mFolder.getName() : "");
                mInfoRow.removeAllViews();
                mSummary.setText("");
                // Fill in default background
                backgroundService.getValue().clearBackgrounds();
                return;
            }

            BaseRowItem rowItem = (BaseRowItem) item;

            mCurrentItem = rowItem;
            mCurrentRow = (ListRow) row;
            mInfoRow.removeAllViews();

            mTitle.setText(rowItem.getName(requireContext()));

            String summary = rowItem.getSummary(requireContext());
            if (summary != null) mSummary.setText(markdownRenderer.getValue().toMarkdownSpanned(summary));
            else mSummary.setText(null);

            InfoLayoutHelper.addInfoRow(requireContext(), rowItem, mInfoRow, true, true);

            ItemRowAdapter adapter = (ItemRowAdapter) ((ListRow) row).getAdapter();
            adapter.loadMoreItemsIfNeeded(rowItem.getIndex());

            backgroundService.getValue().setBackground(rowItem.getBaseItem());
        }
    }
}
