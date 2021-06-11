package org.jellyfin.androidtv.ui.browsing;

import static org.koin.java.KoinJavaComponent.get;
import static org.koin.java.KoinJavaComponent.inject;

import android.content.Intent;
import android.graphics.BlendMode;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;

import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.constant.Extras;
import org.jellyfin.androidtv.constant.GridDirection;
import org.jellyfin.androidtv.constant.ImageType;
import org.jellyfin.androidtv.constant.PosterSize;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.FilterOptions;
import org.jellyfin.androidtv.data.querying.ViewQuery;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.ui.AlphaPicker;
import org.jellyfin.androidtv.ui.GridFragment;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.preference.PreferencesActivity;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.HorizontalGridPresenter;
import org.jellyfin.androidtv.ui.search.SearchActivity;
import org.jellyfin.androidtv.ui.shared.BaseActivity;
import org.jellyfin.androidtv.ui.shared.IKeyListener;
import org.jellyfin.androidtv.ui.shared.IMessageListener;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.entities.CollectionType;
import org.jellyfin.apiclient.model.entities.DisplayPreferences;
import org.jellyfin.apiclient.serialization.GsonJsonSerializer;

import java.util.HashMap;

import kotlin.Lazy;
import timber.log.Timber;

public class StdGridFragment extends GridFragment implements IGridLoader {
    protected String MainTitle;
    protected BaseActivity mActivity;
    protected BaseRowItem mCurrentItem;
    protected CompositeClickedListener mClickedListener = new CompositeClickedListener();
    protected CompositeSelectedListener mSelectedListener = new CompositeSelectedListener();
    protected ItemRowAdapter mGridAdapter;
    private final Handler mHandler = new Handler();
    protected BrowseRowDef mRowDef;
    CardPresenter mCardPresenter;

    protected boolean justLoaded = true;
    protected String mPosterSizeSetting = PosterSize.AUTO;
    protected String mImageType = ImageType.DEFAULT;
    protected String mGridDirection = GridDirection.HORIZONTAL.name();
    protected boolean determiningPosterSize = false;

    protected String mParentId;
    protected BaseItemDto mFolder;
    protected DisplayPreferences mDisplayPrefs;

    private int mCardHeight = SMALL_CARD;

    protected boolean mAllowViewSelection = true;
    private Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private Lazy<MediaManager> mediaManager = inject(MediaManager.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFolder = get(GsonJsonSerializer.class).DeserializeFromString(getActivity().getIntent().getStringExtra(Extras.Folder), BaseItemDto.class);
        mParentId = mFolder.getId();
        MainTitle = mFolder.getName();
        mDisplayPrefs = TvApp.getApplication().getCachedDisplayPrefs(mFolder.getDisplayPreferencesId()); //These should have already been loaded
        mPosterSizeSetting = mDisplayPrefs.getCustomPrefs().get("PosterSize");
        mImageType = mDisplayPrefs.getCustomPrefs().get("ImageType");
        mGridDirection = mDisplayPrefs.getCustomPrefs().get("GridDirection");
        if (mImageType == null) mImageType = ImageType.DEFAULT;
        if (mPosterSizeSetting == null) mPosterSizeSetting = PosterSize.AUTO;
        if (mGridDirection == null) mGridDirection = GridDirection.HORIZONTAL.name();
        
        if (mGridDirection.equals(GridDirection.VERTICAL.name()))
            setGridPresenter(new VerticalGridPresenter());
        else
            setGridPresenter(new HorizontalGridPresenter());

        mCardHeight = getCardHeight(mPosterSizeSetting);
        setCardHeight(mCardHeight);

        setGridSizes();

        mJumplistPopup = new JumplistPopup();
    }

    private void setGridSizes() {
        Presenter gridPresenter = getGridPresenter();

        if (gridPresenter instanceof HorizontalGridPresenter) {
            ((HorizontalGridPresenter) gridPresenter).setNumberOfRows(getGridHeight() / getCardHeight());
        } else if (gridPresenter instanceof VerticalGridPresenter) {
            // Why is this hardcoded you ask? Well did you ever look at getGridHeight()? Yup that one is hardcoded too
            // This whole fragment is only optimized for 16:9 screens anyway
            // is this bad? Yup it definitely is, we'll fix it when this screen is rewritten

            int size;
            switch (mImageType) {
                case ImageType.DEFAULT:
                default:
                    if (mCardHeight == SMALL_VERTICAL_POSTER) {
                        size = 10;
                    } else if (mCardHeight == MED_VERTICAL_POSTER || mCardHeight == SMALL_VERTICAL_SQUARE) {
                        size = 7;
                    } else if (mCardHeight == LARGE_VERTICAL_POSTER) {
                        size = 6;
                    } else if (mCardHeight == MED_VERTICAL_SQUARE) {
                        size = 5;
                    } else {
                        size = 4;
                    }
                    break;
                case ImageType.THUMB:
                    if (mCardHeight == SMALL_VERTICAL_THUMB) {
                        size = 4;
                    } else if (mCardHeight == MED_VERTICAL_THUMB) {
                        size = 3;
                    } else {
                        size = 2;
                    }
                    break;
                case ImageType.BANNER:
                    if (mCardHeight == SMALL_VERTICAL_BANNER) {
                        size = 3;
                    } else if (mCardHeight == MED_VERTICAL_BANNER) {
                        size = 2;
                    } else {
                        size = 1;
                    }
                    break;
            }

            ((VerticalGridPresenter) gridPresenter).setNumberOfColumns(size);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() instanceof BaseActivity) mActivity = (BaseActivity)getActivity();

        backgroundService.getValue().attach(requireActivity());

        setupQueries(this);

        addTools();

        setupEventListeners();
    }

    protected void setupQueries(IGridLoader gridLoader) {
    }

    @Override
    public void onResume() {
        super.onResume();

        String imageType = mDisplayPrefs.getCustomPrefs().get("ImageType");
        if (imageType == null) imageType = ImageType.DEFAULT;
        String posterSizeSetting = mDisplayPrefs.getCustomPrefs().get("PosterSize");
        if (posterSizeSetting == null) posterSizeSetting = PosterSize.AUTO;
        String gridDirection = mDisplayPrefs.getCustomPrefs().get("GridDirection");
        if (gridDirection == null) gridDirection = GridDirection.HORIZONTAL.name();

        if (mImageType != imageType || mPosterSizeSetting != posterSizeSetting || mGridDirection != gridDirection) {
            mImageType = imageType;
            mPosterSizeSetting = posterSizeSetting;
            mGridDirection = gridDirection;

            if (mGridDirection.equals(GridDirection.VERTICAL.name()) && (getGridPresenter() == null || !(getGridPresenter() instanceof VerticalGridPresenter)))
                setGridPresenter(new VerticalGridPresenter());
            else if (mGridDirection.equals(GridDirection.HORIZONTAL.name()) && (getGridPresenter() == null || !(getGridPresenter() instanceof HorizontalGridPresenter)))
                setGridPresenter(new HorizontalGridPresenter());

            int cardHeight = getCardHeight(mPosterSizeSetting);
            if (mCardHeight != cardHeight) {
                mCardHeight = cardHeight;
                setCardHeight(mCardHeight);
            }

            setGridSizes();
            createGrid();
            loadGrid(mRowDef);
        }

        if (!justLoaded) {
            //Re-retrieve anything that needs it but delay slightly so we don't take away gui landing
            if (mGridAdapter != null) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mActivity == null || mActivity.isFinishing()) return;
                        if (mGridAdapter != null && mGridAdapter.size() > 0) {
                            if (!mGridAdapter.ReRetrieveIfNeeded()) refreshCurrentItem();
                        }
                    }
                },500);
            }

        } else {
            justLoaded = false;
        }
    }

    public int getCardHeight() {
        return mCardHeight;
    }

    protected void buildAdapter(BrowseRowDef rowDef) {
        mCardPresenter = new CardPresenter(false, mImageType, mCardHeight);

        switch (mRowDef.getQueryType()) {
            case NextUp:
                mGridAdapter = new ItemRowAdapter(mRowDef.getNextUpQuery(), true, mCardPresenter, null);
                break;
            case Season:
                mGridAdapter = new ItemRowAdapter(mRowDef.getSeasonQuery(), mCardPresenter, null);
                break;
            case Upcoming:
                mGridAdapter = new ItemRowAdapter(mRowDef.getUpcomingQuery(), mCardPresenter, null);
                break;
            case Views:
                mGridAdapter = new ItemRowAdapter(new ViewQuery(), mCardPresenter, null);
                break;
            case SimilarSeries:
                mGridAdapter = new ItemRowAdapter(mRowDef.getSimilarQuery(), QueryType.SimilarSeries, mCardPresenter, null);
                break;
            case SimilarMovies:
                mGridAdapter = new ItemRowAdapter(mRowDef.getSimilarQuery(), QueryType.SimilarMovies, mCardPresenter, null);
                break;
            case Persons:
                mGridAdapter = new ItemRowAdapter(mRowDef.getPersonsQuery(), mRowDef.getChunkSize(), mCardPresenter, null);
                break;
            case LiveTvChannel:
                mGridAdapter = new ItemRowAdapter(mRowDef.getTvChannelQuery(), 40, mCardPresenter, null);
                break;
            case LiveTvProgram:
                mGridAdapter = new ItemRowAdapter(mRowDef.getProgramQuery(), mCardPresenter, null);
                break;
            case LiveTvRecording:
                mGridAdapter = new ItemRowAdapter(mRowDef.getRecordingQuery(), mRowDef.getChunkSize(), mCardPresenter, null);
                break;
            case LiveTvRecordingGroup:
                mGridAdapter = new ItemRowAdapter(mRowDef.getRecordingGroupQuery(), mCardPresenter, null);
                break;
            case AlbumArtists:
                mGridAdapter = new ItemRowAdapter(mRowDef.getArtistsQuery(), mRowDef.getChunkSize(), mCardPresenter, null);
                break;
            default:
                mGridAdapter = new ItemRowAdapter(mRowDef.getQuery(), mRowDef.getChunkSize(), mRowDef.getPreferParentThumb(), mRowDef.isStaticHeight(), mCardPresenter, null);
                break;
        }

        FilterOptions filters = new FilterOptions();
        filters.setFavoriteOnly(Boolean.parseBoolean(mDisplayPrefs.getCustomPrefs().get("FavoriteOnly")));
        filters.setUnwatchedOnly(Boolean.parseBoolean(mDisplayPrefs.getCustomPrefs().get("UnwatchedOnly")));

        setupRetrieveListeners();
        mGridAdapter.setFilters(filters);
        setAdapter(mGridAdapter);
    }

    public void loadGrid(final BrowseRowDef rowDef) {
        determiningPosterSize = true;
        buildAdapter(rowDef);

        if (mPosterSizeSetting.equals(PosterSize.AUTO)) {
            // Use "medium" cards by default
            int autoHeight = getCardHeight(PosterSize.MED);
            if (autoHeight != mCardHeight) {
                mCardHeight = autoHeight;
                setCardHeight(mCardHeight);

                setGridSizes();
                createGrid();
                Timber.d("Auto card height is %d", mCardHeight);
                buildAdapter(rowDef);
            }
        }

        mGridAdapter.setSortBy(getSortOption(mDisplayPrefs.getSortBy()));
        mGridAdapter.Retrieve();
        determiningPosterSize = false;
    }

    protected int getCardHeight(String heightSetting) {
        if (getGridPresenter() instanceof VerticalGridPresenter) {
            boolean isSquareCard = mFolder.getCollectionType().equals(CollectionType.Music);
            switch (heightSetting) {
                case PosterSize.MED:
                    return mImageType.equals(ImageType.BANNER) ? MED_VERTICAL_BANNER : mImageType.equals(ImageType.THUMB) ? MED_VERTICAL_THUMB : (isSquareCard) ? MED_VERTICAL_SQUARE : MED_VERTICAL_POSTER;
                case PosterSize.LARGE:
                    return mImageType.equals(ImageType.BANNER) ? LARGE_VERTICAL_BANNER : mImageType.equals(ImageType.THUMB) ? LARGE_VERTICAL_THUMB : (isSquareCard) ? LARGE_VERTICAL_SQUARE : LARGE_VERTICAL_POSTER;
                default:
                    return mImageType.equals(ImageType.BANNER) ? SMALL_VERTICAL_BANNER : mImageType.equals(ImageType.THUMB) ? SMALL_VERTICAL_THUMB : (isSquareCard) ? SMALL_VERTICAL_SQUARE : SMALL_VERTICAL_POSTER;
            }
        } else {
            switch (heightSetting) {
                case PosterSize.MED:
                    return mImageType.equals(ImageType.BANNER) ? MED_BANNER : MED_CARD;
                case PosterSize.LARGE:
                    return mImageType.equals(ImageType.BANNER) ? LARGE_BANNER : LARGE_CARD;
                default:
                    return mImageType.equals(ImageType.BANNER) ? SMALL_BANNER : SMALL_CARD;
            }
        }
    }

    protected ImageButton mSortButton;
    protected ImageButton mSearchButton;
    protected ImageButton mSettingsButton;
    protected ImageButton mUnwatchedButton;
    protected ImageButton mFavoriteButton;
    protected ImageButton mLetterButton;

    protected void updateDisplayPrefs() {
        if (mDisplayPrefs.getCustomPrefs() == null)
            mDisplayPrefs.setCustomPrefs(new HashMap<String, String>());
        mDisplayPrefs.getCustomPrefs().put("UnwatchedOnly", mGridAdapter.getFilters().isUnwatchedOnly() ? "true" : "false");
        mDisplayPrefs.getCustomPrefs().put("FavoriteOnly", mGridAdapter.getFilters().isFavoriteOnly() ? "true" : "false");
        mDisplayPrefs.setSortBy(mGridAdapter.getSortBy());
        mDisplayPrefs.setSortOrder(getSortOption(mGridAdapter.getSortBy()).order);
        TvApp.getApplication().updateDisplayPrefs(mDisplayPrefs);
    }

    protected void addTools() {
        //Add tools
        LinearLayout toolBar = getToolBar();
        int size = Utils.convertDpToPixel(requireContext(), 26);

        mSortButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
        mSortButton.setImageResource(R.drawable.ic_sort);
        mSortButton.setMaxHeight(size);
        mSortButton.setAdjustViewBounds(true);
        mSortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create sort menu
                PopupMenu sortMenu = Utils.createPopupMenu(getActivity(), getToolBar(), Gravity.RIGHT);
                for (Integer key : sortOptions.keySet()) {
                    SortOption option = sortOptions.get(key);
                    if (option == null) option = sortOptions.get(0);
                    MenuItem item = sortMenu.getMenu().add(0, key, key, option.name);
                    if (option.value.equals(mDisplayPrefs.getSortBy())) item.setChecked(true);
                }
                sortMenu.getMenu().setGroupCheckable(0, true, true);
                sortMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mGridAdapter.setSortBy(sortOptions.get(item.getItemId()));
                        mGridAdapter.Retrieve();
                        item.setChecked(true);
                        updateDisplayPrefs();
                        return true;
                    }
                });
                sortMenu.show();
            }
        });
        mSortButton.setContentDescription(getString(R.string.lbl_sort_by));

        toolBar.addView(mSortButton);

        if (mRowDef.getQueryType() == QueryType.Items) {
            mUnwatchedButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
            mUnwatchedButton.setImageTintBlendMode(BlendMode.DST);
            mUnwatchedButton.setImageResource(mGridAdapter.getFilters().isUnwatchedOnly() ? R.drawable.ic_unwatch_red : R.drawable.ic_unwatch);
            mUnwatchedButton.setMaxHeight(size);
            mUnwatchedButton.setAdjustViewBounds(true);
            mUnwatchedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FilterOptions filters = mGridAdapter.getFilters();
                    if (filters == null) filters = new FilterOptions();

                    filters.setUnwatchedOnly(!filters.isUnwatchedOnly());
                    updateDisplayPrefs();
                    mGridAdapter.setFilters(filters);
                    if (mPosterSizeSetting.equals(PosterSize.AUTO)) {
                        loadGrid(mRowDef);
                    } else {
                        mGridAdapter.Retrieve();
                    }
                    mUnwatchedButton.setImageResource(filters.isUnwatchedOnly() ? R.drawable.ic_unwatch_red : R.drawable.ic_unwatch);


                }
            });
            mUnwatchedButton.setContentDescription(getString(R.string.lbl_unwatched));
            toolBar.addView(mUnwatchedButton);
        }

        mFavoriteButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
        mFavoriteButton.setImageTintBlendMode(BlendMode.DST);
        mFavoriteButton.setImageResource(mGridAdapter.getFilters().isFavoriteOnly() ? R.drawable.ic_heart_red : R.drawable.ic_heart);
        mFavoriteButton.setMaxHeight(size);
        mFavoriteButton.setAdjustViewBounds(true);
        mFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FilterOptions filters = mGridAdapter.getFilters();
                if (filters == null) filters = new FilterOptions();

                filters.setFavoriteOnly(!filters.isFavoriteOnly());
                mGridAdapter.setFilters(filters);
                updateDisplayPrefs();
                if (mPosterSizeSetting.equals(PosterSize.AUTO)) {
                    loadGrid(mRowDef);
                } else {
                    mGridAdapter.Retrieve();
                }
                mFavoriteButton.setImageResource(filters.isFavoriteOnly() ? R.drawable.ic_heart_red : R.drawable.ic_heart);

            }
        });
        mFavoriteButton.setContentDescription(getString(R.string.lbl_favorite));
        toolBar.addView(mFavoriteButton);

        mLetterButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
        mLetterButton.setImageResource(R.drawable.ic_jump_letter);
        mLetterButton.setMaxHeight(size);
        mLetterButton.setAdjustViewBounds(true);
        mLetterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Open letter jump popup
                mJumplistPopup.show();
            }
        });
        mLetterButton.setContentDescription(getString(R.string.lbl_by_letter));
        toolBar.addView(mLetterButton);

        mSearchButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
        mSearchButton.setImageResource(R.drawable.ic_search);
        mSearchButton.setMaxHeight(size);
        mSearchButton.setAdjustViewBounds(true);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                intent.putExtra("MusicOnly", "music".equals(mFolder.getCollectionType()) || mFolder.getBaseItemType() == BaseItemType.MusicAlbum || mFolder.getBaseItemType() == BaseItemType.MusicArtist);

                startActivity(intent);
            }
        });
        mSearchButton.setContentDescription(getString(R.string.lbl_search));
        toolBar.addView(mSearchButton);

        mSettingsButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
        mSettingsButton.setImageResource(R.drawable.ic_settings);
        mSettingsButton.setMaxHeight(size);
        mSettingsButton.setAdjustViewBounds(true);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(getActivity(), PreferencesActivity.class);
                settingsIntent.putExtra(PreferencesActivity.EXTRA_SCREEN, DisplayPreferencesScreen.class.getCanonicalName());
                Bundle screenArgs = new Bundle();
                screenArgs.putString(DisplayPreferencesScreen.ARG_PREFERENCES_ID, mFolder.getDisplayPreferencesId());
                screenArgs.putBoolean(DisplayPreferencesScreen.ARG_ALLOW_VIEW_SELECTION, mAllowViewSelection);
                settingsIntent.putExtra(PreferencesActivity.EXTRA_SCREEN_ARGS, screenArgs);
                getActivity().startActivity(settingsIntent);
            }
        });
        mSettingsButton.setContentDescription(getString(R.string.lbl_settings));
        toolBar.addView(mSettingsButton);
    }

    private JumplistPopup mJumplistPopup;
    class JumplistPopup {

        private final int WIDTH = Utils.convertDpToPixel(requireContext(), 900);
        private final int HEIGHT = Utils.convertDpToPixel(requireContext(), 55);

        private final PopupWindow popupWindow;
        private final AlphaPicker alphaPicker;

        JumplistPopup() {
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            View layout = inflater.inflate(R.layout.popup_empty, mGridDock, false);
            popupWindow = new PopupWindow(layout, WIDTH, HEIGHT, true);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // necessary for popup to dismiss
            popupWindow.setAnimationStyle(R.style.WindowAnimation_SlideTop);

            alphaPicker = new AlphaPicker(requireContext(), null);
            alphaPicker.setOnAlphaSelected(letter -> {
                mGridAdapter.setStartLetter(letter.toString());
                loadGrid(mRowDef);
                dismiss();
                return null;
            });

            FrameLayout root = layout.findViewById(R.id.empty_popup);
            root.addView(alphaPicker);
        }

        public void show() {
            popupWindow.showAtLocation(mGridDock, Gravity.TOP, mGridDock.getLeft(), mGridDock.getTop());
            if (mGridAdapter.getStartLetter() != null && !mGridAdapter.getStartLetter().isEmpty()) {
                alphaPicker.focus(mGridAdapter.getStartLetter().charAt(0));
            }
        }

        public void dismiss() {
            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss();
            }
        }
    }

    protected void setupEventListeners() {

        setOnItemViewClickedListener(mClickedListener);
        mClickedListener.registerListener(new ItemViewClickedListener());

        setOnItemViewSelectedListener(mSelectedListener);
        mSelectedListener.registerListener(new ItemViewSelectedListener());

        if (mActivity != null) {
            mActivity.registerKeyListener(new IKeyListener() {
                @Override
                public boolean onKeyUp(int key, KeyEvent event) {
                    if (key == KeyEvent.KEYCODE_MEDIA_PLAY || key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                        mediaManager.getValue().setCurrentMediaAdapter(mGridAdapter);
                        mediaManager.getValue().setCurrentMediaPosition(mCurrentItem.getIndex());
                        mediaManager.getValue().setCurrentMediaTitle(mFolder.getName());
                    }
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

    protected void setupRetrieveListeners() {
        mGridAdapter.setRetrieveFinishedListener(new EmptyResponse() {
            @Override
            public void onResponse() {
                setStatusText(mFolder.getName());
                updateCounter(mGridAdapter.getTotalItems() > 0 ? 1 : 0);
                mLetterButton.setVisibility("SortName".equals(mGridAdapter.getSortBy()) ? View.VISIBLE : View.GONE);
                setItem(null);
                if (mGridAdapter.getTotalItems() == 0) {
                    mToolBar.requestFocus();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setTitle(mFolder.getName());

                        }
                    }, 500);
                } else focusGrid();
            }
        });
    }

    private void refreshCurrentItem() {
        if (mediaManager.getValue().getCurrentMediaPosition() >= 0) {
            mCurrentItem = mediaManager.getValue().getCurrentMediaItem();

            Presenter presenter = getGridPresenter();
            if (presenter instanceof HorizontalGridPresenter)
                ((HorizontalGridPresenter) presenter).setPosition(mediaManager.getValue().getCurrentMediaPosition());
            // Don't do anything for vertical grids as the presenter does not allow setting the position

            mediaManager.getValue().setCurrentMediaPosition(-1); // re-set so it doesn't mess with parent views
        }
        if (mCurrentItem != null && mCurrentItem.getBaseItemType() != BaseItemType.Photo && mCurrentItem.getBaseItemType() != BaseItemType.PhotoAlbum
                && mCurrentItem.getBaseItemType() != BaseItemType.MusicArtist && mCurrentItem.getBaseItemType() != BaseItemType.MusicAlbum) {
            Timber.d("Refresh item \"%s\"", mCurrentItem.getFullName());
            mCurrentItem.refresh(new EmptyResponse() {
                @Override
                public void onResponse() {

                    mGridAdapter.notifyArrayItemRangeChanged(mGridAdapter.indexOf(mCurrentItem), 1);
                    //Now - if filtered make sure we still pass
                    if (mGridAdapter.getFilters() != null) {
                        if ((mGridAdapter.getFilters().isFavoriteOnly() && !mCurrentItem.isFavorite()) || (mGridAdapter.getFilters().isUnwatchedOnly() && mCurrentItem.isPlayed())) {
                            //if we are about to remove last item, throw focus to toolbar so framework doesn't crash
                            if (mGridAdapter.size() == 1) mToolBar.requestFocus();
                            mGridAdapter.remove(mCurrentItem);
                            mGridAdapter.setTotalItems(mGridAdapter.getTotalItems() - 1);
                            updateCounter(mCurrentItem.getIndex());
                        }
                    }
                }
            });
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            ItemLauncher.launch((BaseRowItem) item, mGridAdapter, ((BaseRowItem)item).getIndex(), getActivity());
        }
    }

    private final Runnable mDelayedSetItem = new Runnable() {
        @Override
        public void run() {
            backgroundService.getValue().setBackground(mCurrentItem.getBaseItem());
            setItem(mCurrentItem);
        }
    };

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {

            mHandler.removeCallbacks(mDelayedSetItem);
            if (!(item instanceof BaseRowItem)) {
                mCurrentItem = null;
                setTitle(MainTitle);
                //fill in default background
                backgroundService.getValue().clearBackgrounds();
            } else {
                mCurrentItem = (BaseRowItem)item;
                mTitleView.setText(mCurrentItem.getName());
                mInfoRow.removeAllViews();
                mHandler.postDelayed(mDelayedSetItem, 400);

                if (!determiningPosterSize) mGridAdapter.loadMoreItemsIfNeeded(mCurrentItem.getIndex());

            }

        }
    }
}
