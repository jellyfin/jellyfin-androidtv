package org.jellyfin.androidtv.ui.itemdetail;

import static org.koin.java.KoinJavaComponent.inject;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import org.jellyfin.androidtv.BuildConfig;
import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.model.PlaylistPaginationState;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.databinding.FragmentItemListBinding;
import org.jellyfin.androidtv.databinding.ViewRowDetailsBinding;
import org.jellyfin.androidtv.ui.AsyncImageView;
import org.jellyfin.androidtv.ui.ItemListView;
import org.jellyfin.androidtv.ui.ItemListViewHelperKt;
import org.jellyfin.androidtv.ui.ItemRowView;
import org.jellyfin.androidtv.ui.TextUnderButton;
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.navigation.Destinations;
import org.jellyfin.androidtv.ui.navigation.NavigationRepository;
import org.jellyfin.androidtv.ui.playback.AudioEventListener;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher;
import org.jellyfin.androidtv.util.ImageHelper;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.PlaybackHelper;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.sdk.BaseItemExtensionsKt;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.MediaType;
import org.koin.java.KoinJavaComponent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import kotlin.Lazy;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import timber.log.Timber;

public class ItemListFragment extends Fragment implements View.OnKeyListener {
    private int BUTTON_SIZE;

    private TextView mTitle;
    private TextView mGenreRow;
    private AsyncImageView mPoster;
    private TextView mSummary;
    private LinearLayout mButtonRow;
    private ItemListView mItemList;
    private ScrollView mScrollView;
    private ItemRowView mCurrentRow;

    private ItemRowView mCurrentlyPlayingRow;

    private BaseItemDto mBaseItem;
    private List<BaseItemDto> mItems = new ArrayList<>();

    private int mBottomScrollThreshold;

    // Pagination fields
    private PlaylistPaginationState mPaginationState;
    private boolean mIsPaginationEnabled = false;

    // Top pagination controls
    private TextView mPaginationInfoTop;
    private LinearLayout mPaginationControlsTop;
    private View mPaginationContainerTop;

    // Bottom pagination controls
    private TextView mPaginationInfo;
    private LinearLayout mPaginationControls;
    private View mPaginationContainer;

    private DisplayMetrics mMetrics;

    private boolean firstTime = true;
    private Instant lastUpdated = Instant.now();

    private final Lazy<DataRefreshService> dataRefreshService = inject(DataRefreshService.class);
    private final Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private final Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    private final Lazy<NavigationRepository> navigationRepository = inject(NavigationRepository.class);
    private final Lazy<ItemLauncher> itemLauncher = inject(ItemLauncher.class);
    private final Lazy<PlaybackHelper> playbackHelper = inject(PlaybackHelper.class);
    private final Lazy<ImageHelper> imageHelper = inject(ImageHelper.class);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentItemListBinding binding = FragmentItemListBinding.inflate(getLayoutInflater(), container, false);

        BUTTON_SIZE = Utils.convertDpToPixel(requireContext(), 35);

        ViewRowDetailsBinding detailsBinding = binding.details.getBinding();
        mTitle = detailsBinding.fdTitle;
        mTitle.setText(getString(R.string.loading));
        mGenreRow = detailsBinding.fdGenreRow;
        mPoster = detailsBinding.mainImage;
        mButtonRow = detailsBinding.fdButtonRow;
        mSummary = detailsBinding.fdSummaryText;
        mItemList = binding.songs;
        mScrollView = binding.scrollView;

        // Initialize pagination UI components
        // Top pagination controls
        mPaginationContainerTop = binding.paginationContainerTop;
        mPaginationInfoTop = binding.paginationContainerTop.findViewById(R.id.paginationInfoTop);
        mPaginationControlsTop = binding.paginationContainerTop.findViewById(R.id.paginationControlsTop);

        // Bottom pagination controls
        mPaginationContainer = binding.paginationContainer;
        mPaginationInfo = binding.paginationContainer.findViewById(R.id.paginationInfo);
        mPaginationControls = binding.paginationContainer.findViewById(R.id.paginationControls);

        // Set pagination container visibility if pagination was enabled before view creation
        if (mIsPaginationEnabled) {
            if (mPaginationContainerTop != null) {
                mPaginationContainerTop.setVisibility(View.VISIBLE);
            }
            if (mPaginationContainer != null) {
                mPaginationContainer.setVisibility(View.VISIBLE);
            }
        }

        mMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        mBottomScrollThreshold = (int) (mMetrics.heightPixels * .6);

        //Item list listeners
        mItemList.setRowSelectedListener(new ItemRowView.RowSelectedListener() {
            @Override
            public void onRowSelected(ItemRowView row) {
                mCurrentRow = row;
                //Keep selected row in center of screen
                int[] location = new int[]{0, 0};
                row.getLocationOnScreen(location);
                int y = location[1];
                if (y > mBottomScrollThreshold) {
                    // too close to bottom - scroll down
                    mScrollView.smoothScrollBy(0, y - mBottomScrollThreshold);
                }
            }
        });

        mItemList.setRowClickedListener(new ItemRowView.RowClickedListener() {
            @Override
            public void onRowClicked(ItemRowView row) {
                showMenu(row, row.getItem().getType() != BaseItemKind.AUDIO);
            }
        });

        // Set up pagination button listeners for both top and bottom controls
        // Bottom controls
        TextView previousButton = mPaginationContainer.findViewById(R.id.previousPageBtn);
        TextView nextButton = mPaginationContainer.findViewById(R.id.nextPageBtn);

        // Top controls
        TextView previousButtonTop = mPaginationContainerTop.findViewById(R.id.previousPageBtnTop);
        TextView nextButtonTop = mPaginationContainerTop.findViewById(R.id.nextPageBtnTop);

        // Bottom button listeners
        previousButton.setOnClickListener(v -> goToPreviousPage());
        nextButton.setOnClickListener(v -> goToNextPage());

        // Top button listeners (mirrors bottom controls)
        previousButtonTop.setOnClickListener(v -> goToPreviousPage());
        nextButtonTop.setOnClickListener(v -> goToNextPage());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        UUID mItemId = Utils.uuidOrNull(getArguments().getString("ItemId"));
        ItemListFragmentHelperKt.loadItem(this, mItemId);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) return false;

        if (mediaManager.getValue().isPlayingAudio()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    mediaManager.getValue().togglePlayPause();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    mediaManager.getValue().nextAudioItem();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    mediaManager.getValue().prevAudioItem();
                    return true;
                case KeyEvent.KEYCODE_MENU:
                    showMenu(mCurrentRow, false);
                    return true;
            }
        } else if (mCurrentRow != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_MENU:
                    showMenu(mCurrentRow, false);
                    return true;
            }
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        mediaManager.getValue().addAudioEventListener(mAudioEventListener);
        // and fire it to be sure we're updated
        mAudioEventListener.onPlaybackStateChange(mediaManager.getValue().isPlayingAudio() ? PlaybackController.PlaybackState.PLAYING : PlaybackController.PlaybackState.IDLE, mediaManager.getValue().getCurrentAudioItem());

        if (!firstTime && dataRefreshService.getValue().getLastPlayback() != null && dataRefreshService.getValue().getLastPlayback().isAfter(lastUpdated)) {
            if (MediaType.VIDEO.equals(mBaseItem.getMediaType())) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                            return;

                        ItemListViewHelperKt.refresh(mItemList);
                        lastUpdated = Instant.now();

                    }
                }, 500);
            }
        }

        firstTime = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        mediaManager.getValue().removeAudioEventListener(mAudioEventListener);
    }

    private AudioEventListener mAudioEventListener = new AudioEventListener() {
        @Override
        public void onPlaybackStateChange(@NonNull PlaybackController.PlaybackState newState, @Nullable BaseItemDto currentItem) {
            Timber.i("Got playback state change event %s for item %s", newState.toString(), currentItem != null ? currentItem.getName() : "<unknown>");

            if (newState != PlaybackController.PlaybackState.PLAYING || currentItem == null) {
                if (mCurrentlyPlayingRow != null) mCurrentlyPlayingRow.updateCurrentTime(-1);
                mCurrentlyPlayingRow = mItemList.updatePlaying(null);
            } else {
                mCurrentlyPlayingRow = mItemList.updatePlaying(currentItem.getId());
            }
        }

        @Override
        public void onProgress(long pos) {
            if (mCurrentlyPlayingRow != null) {
                mCurrentlyPlayingRow.updateCurrentTime(pos);
            }
        }

        @Override
        public void onQueueStatusChanged(boolean hasQueue) {
        }

        @Override
        public void onQueueReplaced() {
        }
    };

    private void showMenu(final ItemRowView row, boolean showOpen) {
        PopupMenu menu = new PopupMenu(requireContext(), row != null ? row : requireActivity().getCurrentFocus(), Gravity.END);
        int order = 0;
        if (showOpen) {
            MenuItem open = menu.getMenu().add(0, 0, order++, R.string.lbl_open);
            open.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    itemLauncher.getValue().launch(new BaseItemDtoBaseRowItem(row.getItem()), null, requireContext());
                    return true;
                }
            });

        }
        MenuItem playFromHere = menu.getMenu().add(0, 0, order++, R.string.lbl_play_from_here);
        playFromHere.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                play(mItems, row.getIndex(), false);
                return true;
            }
        });
        MenuItem play = menu.getMenu().add(0, 1, order++, R.string.lbl_play);
        play.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                play(mItems.subList(row.getIndex(), row.getIndex() + 1), false);
                return true;
            }
        });
        if (row.getItem().getType() == BaseItemKind.AUDIO) {
            MenuItem queue = menu.getMenu().add(0, 2, order++, R.string.lbl_add_to_queue);
            queue.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    mediaManager.getValue().queueAudioItem(row.getItem());
                    return true;
                }
            });

            MenuItem mix = menu.getMenu().add(0, 1, order++, R.string.lbl_instant_mix);
            mix.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    playbackHelper.getValue().playInstantMix(requireContext(), row.getItem());
                    return true;
                }
            });

        }

        menu.show();
    }

    public void setBaseItem(BaseItemDto item) {
        mBaseItem = item;

        // Initialize pagination for playlists only (not for music albums or other types)
        mIsPaginationEnabled = (item.getType() == BaseItemKind.PLAYLIST);
        if (mIsPaginationEnabled) {
            mPaginationState = new PlaylistPaginationState(1, 0, 100, false);
            if (BuildConfig.DEBUG) {
                Timber.d("Pagination enabled for playlist: %s", item.getName());
            }
        }

        // Set visibility of pagination containers if views are ready
        if (mPaginationContainerTop != null) {
            mPaginationContainerTop.setVisibility(mIsPaginationEnabled ? View.VISIBLE : View.GONE);
        }

        if (mPaginationContainer != null) {
            mPaginationContainer.setVisibility(mIsPaginationEnabled ? View.VISIBLE : View.GONE);
        }

        LinearLayout mainInfoRow = requireActivity().findViewById(R.id.fdMainInfoRow);

        InfoLayoutHelper.addInfoRow(requireContext(), item, mainInfoRow, false);
        addGenres(mGenreRow);
        addButtons(BUTTON_SIZE);
        mSummary.setText(mBaseItem.getOverview());

        Double aspect = imageHelper.getValue().getImageAspectRatio(item, false);
        String primaryImageUrl = imageHelper.getValue().getPrimaryImageUrl(item, null, ImageHelper.MAX_PRIMARY_IMAGE_HEIGHT);
        mPoster.setPadding(0, 0, 0, 0);
        mPoster.load(primaryImageUrl, null, ContextCompat.getDrawable(requireContext(), R.drawable.ic_album), aspect, 0);

        // Load items with pagination if enabled
        if (mIsPaginationEnabled) {
            loadPlaylistPage(mPaginationState);
        } else {
            ItemListFragmentHelperKt.getPlaylist(this, mBaseItem, itemResponse);
        }
    }

    private Function1<List<BaseItemDto>, Unit> itemResponse = (List<BaseItemDto> items) -> {
        mTitle.setText(mBaseItem.getName());
        if (mBaseItem.getName().length() > 32) {
            // scale down the title so more will fit
            mTitle.setTextSize(32);
        }
        if (!items.isEmpty()) {
            mItems = new ArrayList<>();
            int i = 0;
            for (BaseItemDto item : items) {
                mItemList.addItem(item, i++);
                mItems.add(item);
            }
            if (mediaManager.getValue().isPlayingAudio()) {
                //update our status
                mAudioEventListener.onPlaybackStateChange(PlaybackController.PlaybackState.PLAYING, mediaManager.getValue().getCurrentAudioItem());
            }

            updateBackdrop();

            // Add Continue button after items are loaded for playlists (non-paginated)
            if (!mIsPaginationEnabled && !items.isEmpty() && mBaseItem.getType() == BaseItemKind.PLAYLIST) {
                addContinueButton();
            }
        }
        return null;
    };

    /**
 * Response handler for paginated playlist API calls.
 * Updates the UI with the current page items and pagination state.
 */
private Function1<PlaylistResult, Unit> paginatedItemResponse = (PlaylistResult result) -> {
        if (BuildConfig.DEBUG) {
            Timber.d("Loaded %d items (total: %d) for page %d",
                result.getItems().size(), result.getTotalItems(), mPaginationState.getCurrentPage());
        }

        // Update UI elements
        mTitle.setText(mBaseItem.getName());
        if (mBaseItem.getName().length() > 32) {
            mTitle.setTextSize(32); // Scale down title for longer names
        }

        // Update pagination state with total item count and reset loading state
        mPaginationState = mPaginationState.withTotalItems(result.getTotalItems())
            .withLoading(false);

        if (!result.getItems().isEmpty()) {
            // Clear and repopulate the current page items
            mItems = new ArrayList<>();
            mItemList.clear();

            // Use correct start index for proper item numbering (1-100, 101-200, etc.)
            int globalIndex = mPaginationState.getStartIndex();
            for (BaseItemDto item : result.getItems()) {
                mItemList.addItem(item, globalIndex++);
                mItems.add(item);
            }

            // Update audio playback status if needed
            if (mediaManager.getValue().isPlayingAudio()) {
                mAudioEventListener.onPlaybackStateChange(PlaybackController.PlaybackState.PLAYING,
                    mediaManager.getValue().getCurrentAudioItem());
            }

            updateBackdrop();
        }

        // Update pagination controls and add Continue button for playlists
        updatePaginationUI();

        // Add Continue button after items are loaded (only for paginated playlists)
        if (mIsPaginationEnabled && !result.getItems().isEmpty()) {
            addContinueButton();
        }

        return null;
    };

    private void loadPlaylistPage(PlaylistPaginationState state) {
        if (state.getIsLoading()) return; // Prevent multiple simultaneous loads

        mPaginationState = state.withLoading(true);
        updatePaginationUI();

        ItemListFragmentHelperKt.getPlaylistPaginated(this, mBaseItem, state, paginatedItemResponse);
    }

    /**
 * Navigates to a specific page and loads its items.
 * @param page The page number to navigate to (1-based)
 */
private void goToPage(int page) {
        if (!mIsPaginationEnabled || mPaginationState == null) return;

        PlaylistPaginationState newState = mPaginationState.withPage(page);
        loadPlaylistPage(newState);
    }

    /**
 * Navigates to the next page if available.
 * Validates pagination state before navigation.
 */
private void goToNextPage() {
        if (!mIsPaginationEnabled || mPaginationState == null || !mPaginationState.hasNextPage()) {
            return;
        }
        goToPage(mPaginationState.getCurrentPage() + 1);
    }

    /**
 * Navigates to the previous page if available.
 * Validates pagination state before navigation.
 */
    private void goToPreviousPage() {
        if (!mIsPaginationEnabled || mPaginationState == null || !mPaginationState.hasPreviousPage()) {
            return;
        }
        goToPage(mPaginationState.getCurrentPage() - 1);
    }

    private void updatePaginationUI() {
        if (!mIsPaginationEnabled) return;

        // Update top pagination controls
        updatePaginationControls(mPaginationInfoTop, mPaginationControlsTop);

        // Update bottom pagination controls
        updatePaginationControls(mPaginationInfo, mPaginationControls);
    }

    private void updatePaginationControls(TextView infoView, LinearLayout controlsView) {
        if (infoView == null || controlsView == null) return;

        if (mPaginationState.getIsLoading()) {
            infoView.setText("Loading...");
            // Disable pagination controls during loading
            for (int i = 0; i < controlsView.getChildCount(); i++) {
                controlsView.getChildAt(i).setEnabled(false);
            }
        } else {
            infoView.setText(mPaginationState.getPageDisplayText());

            // Enable/disable navigation buttons
            for (int i = 0; i < controlsView.getChildCount(); i++) {
                View child = controlsView.getChildAt(i);
                boolean enabled = true;

                if (i == 0) { // Previous button
                    enabled = mPaginationState.hasPreviousPage();
                } else if (i == 2) { // Next button
                    enabled = mPaginationState.hasNextPage();
                }

                child.setEnabled(enabled);
            }
        }
    }

    private int findFirstUnwatchedItemIndex() {
        if (mItems == null || mItems.isEmpty()) {
            return 0;
        }

        // First, look for a partially watched item (has some playback progress but not completed)
        for (int i = 0; i < mItems.size(); i++) {
            BaseItemDto item = mItems.get(i);
            if (item.getUserData() != null &&
                !item.getUserData().getPlayed() &&
                item.getUserData().getPlaybackPositionTicks() > 0) {
                Timber.d("Found partially watched item at index %d: %s (position: %d ticks)",
                    i, item.getName(), item.getUserData().getPlaybackPositionTicks());
                return i;
            }
        }

        // If no partially watched items, look for first completely unwatched item
        for (int i = 0; i < mItems.size(); i++) {
            BaseItemDto item = mItems.get(i);
            if (item.getUserData() != null && !item.getUserData().getPlayed()) {
                Timber.d("Found unwatched item at index %d: %s", i, item.getName());
                return i;
            }
        }

        // If all items are watched or no user data, return first item
        Timber.d("No unwatched items found in current page, defaulting to first item");
        return 0;
    }

    /**
 * Adds a "Continue" button to playlists that finds and plays the first unwatched item.
 * The button searches across all pages of the playlist and navigates to the correct page if needed.
 */
private void addContinueButton() {
        // Only add Continue button for playlists with items
        if (mBaseItem.getType() != BaseItemKind.PLAYLIST || mItems == null || mItems.isEmpty()) {
            return;
        }

        if (BuildConfig.DEBUG) {
            Timber.d("Adding Continue button for playlist with %d items", mItems.size());
        }

        int buttonSize = Utils.convertDpToPixel(requireContext(), 35);
        TextUnderButton continueButton = TextUnderButton.create(requireContext(), R.drawable.ic_play, buttonSize, 2,
            getString(R.string.lbl_continue), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BuildConfig.DEBUG) {
                    Timber.d("Continue button clicked - searching for first unwatched item");
                }

                // Show user feedback while searching
                Utils.showToast(requireContext(), "Searching for unwatched items...");

                // Search for first unwatched item across entire playlist using efficient API filter
                ItemListFragmentHelperKt.findFirstUnwatchedItemInPlaylist(ItemListFragment.this, mBaseItem.getId(), (firstUnwatchedItem) -> {
                    if (firstUnwatchedItem != null) {
                        if (BuildConfig.DEBUG) {
                            Timber.d("Found unwatched item: %s", firstUnwatchedItem.getName());
                        }

                        // Check if the unwatched item is on the current page
                        int currentItemIndex = findItemInCurrentPage(firstUnwatchedItem.getId());

                        if (currentItemIndex != -1) {
                            // Item is on current page, play it directly
                            if (BuildConfig.DEBUG) {
                                Timber.d("Unwatched item found on current page at index %d", currentItemIndex);
                            }
                            play(mItems, currentItemIndex, false);
                        } else {
                            // Item is on a different page, navigate to the correct page first
                            if (BuildConfig.DEBUG) {
                                Timber.d("Unwatched item on different page - navigating");
                            }
                            navigateToItemAndPlay(firstUnwatchedItem);
                        }
                    } else {
                        // No unwatched items found, fall back to playing from beginning
                        if (BuildConfig.DEBUG) {
                            Timber.d("No unwatched items found, playing from beginning");
                        }
                        Utils.showToast(requireContext(), "No unwatched items found, playing from beginning");
                        play(mItems, 0, false);
                    }
                    return null;
                });
            }
        });

        // Add focus handling for TV navigation
        continueButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) mScrollView.smoothScrollTo(0, 0);
        });

        mButtonRow.addView(continueButton);

        if (BuildConfig.DEBUG) {
            Timber.d("Continue button added successfully");
        }
    }

    /**
 * Searches for a specific item in the currently loaded page.
 * @param itemId The UUID of the item to find
 * @return Index of the item in current page, or -1 if not found
 */
private int findItemInCurrentPage(UUID itemId) {
        if (mItems == null || mItems.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).getId().equals(itemId)) {
                return i;
            }
        }
        return -1;
    }

/**
 * Navigates to the correct page containing the target item and starts playback.
 * Loads the full playlist to determine which page the item is on, then navigates and plays.
 * @param targetItem The BaseItemDto to navigate to and play
 */
private void navigateToItemAndPlay(BaseItemDto targetItem) {
        if (BuildConfig.DEBUG) {
            Timber.d("Navigating to item: %s", targetItem.getName());
        }

        Utils.showToast(requireContext(), "Loading item page...");

        // Load full playlist items to determine target item's page
        // Use high limit (5000) to ensure we get all items for accurate page calculation
        org.jellyfin.androidtv.data.model.PlaylistPaginationState searchState =
            new org.jellyfin.androidtv.data.model.PlaylistPaginationState(1, 0, 5000, false);

        ItemListFragmentHelperKt.getPlaylistPaginated(this, mBaseItem, searchState, (result) -> {
            int targetIndex = -1;

            // Find the target item's global index in the full playlist
            for (int i = 0; i < result.getItems().size(); i++) {
                if (result.getItems().get(i).getId().equals(targetItem.getId())) {
                    targetIndex = i;
                    break;
                }
            }

            if (targetIndex != -1) {
                // Calculate which page contains the target item (100 items per page)
                int pageSize = 100;
                int targetPage = (targetIndex / pageSize) + 1;

                if (BuildConfig.DEBUG) {
                    Timber.d("Target item on page %d (global index: %d)", targetPage, targetIndex);
                }

                // Navigate to the correct page if pagination is enabled
                if (mIsPaginationEnabled && mPaginationState != null) {
                    PlaylistPaginationState newState = mPaginationState.withPage(targetPage);
                    loadPlaylistPage(newState);

                    // Wait for page to load, then find and play the target item
                    new android.os.Handler().postDelayed(() -> {
                        int pageItemIndex = findItemInCurrentPage(targetItem.getId());
                        if (pageItemIndex != -1) {
                            Utils.showToast(requireContext(), "Playing: " + targetItem.getName());
                            play(mItems, pageItemIndex, false);
                        } else {
                            Utils.showToast(requireContext(), "Error loading item page");
                        }
                    }, 1500); // Allow time for page loading
                } else {
                    // Play directly if pagination is not enabled
                    Utils.showToast(requireContext(), "Playing: " + targetItem.getName());
                    play(result.getItems(), targetIndex, false);
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Timber.d("Target item not found in playlist of %d items", result.getItems().size());
                }
                Utils.showToast(requireContext(), "Item not found in playlist");
            }
            return null;
        });
    }

    private void addGenres(TextView textView) {
        List<String> genres = mBaseItem.getGenres();
        if (genres != null) textView.setText(TextUtils.join(" / ", genres));
        else textView.setText(null);
    }

    private void play(List<BaseItemDto> items, boolean shuffle) {
        play(items, 0, shuffle);
    }

    private void play(List<BaseItemDto> items, int ndx, boolean shuffle) {
        Timber.d("play items: %d, ndx: %d, shuffle: %b", items.size(), ndx, shuffle);

        int pos = 0;
        BaseItemDto item = items.size() > 0 ? items.get(ndx) : null;
        if (item != null && item.getUserData() != null) {
            pos = Math.toIntExact(item.getUserData().getPlaybackPositionTicks() / 10000);
        }
        KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).launch(getContext(), items, pos, false, ndx, shuffle);
    }

    private void addButtons(int buttonSize) {
        if (BaseItemExtensionsKt.canPlay(mBaseItem)) {
            // add play button but don't show and focus yet
            TextUnderButton play = TextUnderButton.create(requireContext(), R.drawable.ic_play, buttonSize, 2, getString(mBaseItem.isFolder() ? R.string.lbl_play_all : R.string.lbl_play), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItems.size() > 0) {
                        play(mItems, false);
                    } else {
                        Utils.showToast(requireContext(), R.string.msg_no_playable_items);
                    }
                }
            });
            play.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) mScrollView.smoothScrollTo(0, 0);
            });
            mButtonRow.addView(play);

            
            boolean hidePlayButton = false;
            TextUnderButton queueButton = null;
            // add to queue if a queue exists and mBaseItem is a MusicAlbum
            if (mBaseItem.getType() == BaseItemKind.MUSIC_ALBUM && mediaManager.getValue().hasAudioQueueItems()) {
                queueButton = TextUnderButton.create(requireContext(), R.drawable.ic_add, buttonSize, 2, getString(R.string.lbl_add_to_queue), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mediaManager.getValue().addToAudioQueue(mItems);
                    }
                });
                hidePlayButton = true;
                mButtonRow.addView(queueButton);
                queueButton.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) mScrollView.smoothScrollTo(0, 0);
                });
            }

            // hide the play button and show add to queue if eligible
            if (hidePlayButton) {
                play.setVisibility(View.GONE);
                queueButton.requestFocus();
            } else {
                play.requestFocus();
            }

            if (mBaseItem.isFolder()) {
                TextUnderButton shuffle = TextUnderButton.create(requireContext(), R.drawable.ic_shuffle, buttonSize, 2, getString(R.string.lbl_shuffle_all), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!mItems.isEmpty()) {
                            //use server retrieval in order to get all items
                            playbackHelper.getValue().retrieveAndPlay(mBaseItem.getId(), true, requireContext());
                        } else {
                            Utils.showToast(requireContext(), R.string.msg_no_playable_items);
                        }
                    }
                });
                mButtonRow.addView(shuffle);
                shuffle.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) mScrollView.smoothScrollTo(0, 0);
                });
            }
        }

        if (mBaseItem.getType() == BaseItemKind.MUSIC_ALBUM) {
            TextUnderButton mix = TextUnderButton.create(requireContext(), R.drawable.ic_mix, buttonSize, 2, getString(R.string.lbl_instant_mix), new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    playbackHelper.getValue().playInstantMix(requireContext(), mBaseItem);
                }
            });
            mButtonRow.addView(mix);
            mix.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) mScrollView.smoothScrollTo(0, 0);
            });
        }

        //Favorite
        TextUnderButton fav = TextUnderButton.create(requireContext(), R.drawable.ic_heart, buttonSize, 2, getString(R.string.lbl_favorite), new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                ItemListFragmentHelperKt.toggleFavorite(ItemListFragment.this, mBaseItem, (BaseItemDto updatedItem) -> {
                    mBaseItem = updatedItem;
                    v.setActivated(mBaseItem.getUserData().isFavorite());
                    dataRefreshService.getValue().setLastFavoriteUpdate(Instant.now());
                    return null;
                });
            }
        });
        fav.setActivated(mBaseItem.getUserData().isFavorite());
        mButtonRow.addView(fav);
        fav.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) mScrollView.smoothScrollTo(0, 0);
        });

        if (mBaseItem.getAlbumArtists() != null && !mBaseItem.getAlbumArtists().isEmpty()) {
            TextUnderButton artist = TextUnderButton.create(requireContext(), R.drawable.ic_user, buttonSize, 4, getString(R.string.lbl_open_artist), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigationRepository.getValue().navigate(Destinations.INSTANCE.itemDetails(mBaseItem.getAlbumArtists().get(0).getId()));
                }
            });
            mButtonRow.addView(artist);
            artist.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) mScrollView.smoothScrollTo(0, 0);
            });
        }

    }

    private void updateBackdrop() {
        BaseItemDto item = mBaseItem;

        if (item.getBackdropImageTags() == null || item.getBackdropImageTags().isEmpty() && mItems != null && !mItems.isEmpty())
            item = mItems.get(new Random().nextInt(mItems.size()));

        backgroundService.getValue().setBackground(item);
    }
}
