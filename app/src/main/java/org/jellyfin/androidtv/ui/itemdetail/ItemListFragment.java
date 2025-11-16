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

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.data.model.DataRefreshService;
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
        public void onProgress(long pos, long duration) {
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

        LinearLayout mainInfoRow = requireActivity().findViewById(R.id.fdMainInfoRow);

        InfoLayoutHelper.addInfoRow(requireContext(), item, mainInfoRow, false);
        addGenres(mGenreRow);
        addButtons(BUTTON_SIZE);
        mSummary.setText(mBaseItem.getOverview());

        Double aspect = imageHelper.getValue().getImageAspectRatio(item, false);
        String primaryImageUrl = imageHelper.getValue().getPrimaryImageUrl(item, null, ImageHelper.MAX_PRIMARY_IMAGE_HEIGHT);
        mPoster.setPadding(0, 0, 0, 0);
        mPoster.load(primaryImageUrl, null, ContextCompat.getDrawable(requireContext(), R.drawable.ic_album), aspect, 0);

        ItemListFragmentHelperKt.getPlaylist(this, mBaseItem, itemResponse);
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
        }
        return null;
    };

    private void addGenres(TextView textView) {
        List<String> genres = mBaseItem.getGenres();
        if (genres != null) textView.setText(TextUtils.join(" / ", genres));
        else textView.setText(null);
    }

    private void play(List<BaseItemDto> items, boolean shuffle) {
        play(items, 0, shuffle);
    }

    private void play(List<BaseItemDto> items, int ndx, boolean shuffle) {
        Timber.i("play items: %d, ndx: %d, shuffle: %b", items.size(), ndx, shuffle);

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
