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
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.databinding.FragmentItemListBinding;
import org.jellyfin.androidtv.databinding.ViewRowDetailsBinding;
import org.jellyfin.androidtv.ui.AsyncImageView;
import org.jellyfin.androidtv.ui.ItemListView;
import org.jellyfin.androidtv.ui.ItemListViewHelperKt;
import org.jellyfin.androidtv.ui.ItemRowView;
import org.jellyfin.androidtv.ui.TextUnderButton;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.navigation.Destination;
import org.jellyfin.androidtv.ui.navigation.Destinations;
import org.jellyfin.androidtv.ui.navigation.NavigationRepository;
import org.jellyfin.androidtv.ui.playback.AudioEventListener;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher;
import org.jellyfin.androidtv.ui.playback.VideoQueueManager;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.LifecycleAwareResponse;
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper;
import org.jellyfin.androidtv.util.sdk.BaseItemExtensionsKt;
import org.jellyfin.androidtv.util.sdk.compat.FakeBaseItem;
import org.jellyfin.androidtv.util.sdk.compat.JavaCompat;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.library.PlayAccess;
import org.jellyfin.apiclient.model.playlists.PlaylistItemQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemFilter;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.constant.ItemSortBy;
import org.jellyfin.sdk.model.constant.MediaType;
import org.jellyfin.sdk.model.serializer.UUIDSerializerKt;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import kotlin.Lazy;
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
    private String mItemId;

    private int mBottomScrollThreshold;

    private DisplayMetrics mMetrics;

    private boolean firstTime = true;
    private Calendar lastUpdated = Calendar.getInstance();

    private final Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private final Lazy<DataRefreshService> dataRefreshService = inject(DataRefreshService.class);
    private Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    private Lazy<VideoQueueManager> videoQueueManager = inject(VideoQueueManager.class);
    private Lazy<NavigationRepository> navigationRepository = inject(NavigationRepository.class);

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

        //adjust left frame
        RelativeLayout leftFrame = detailsBinding.leftFrame;
        ViewGroup.LayoutParams params = leftFrame.getLayoutParams();
        params.width = Utils.convertDpToPixel(requireContext(),100);


        mMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        mBottomScrollThreshold = (int)(mMetrics.heightPixels *.6);

        //Item list listeners
        mItemList.setRowSelectedListener(new ItemRowView.RowSelectedListener() {
            @Override
            public void onRowSelected(ItemRowView row) {
                mCurrentRow = row;
                //Keep selected row in center of screen
                int[] location = new int[] {0,0};
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

        mItemId = getArguments().getString("ItemId");
        loadItem(mItemId);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) return false;

        if (mediaManager.getValue().isPlayingAudio()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    if (mediaManager.getValue().isPlayingAudio()) mediaManager.getValue().pauseAudio();
                    else mediaManager.getValue().resumeAudio();
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

        if (!firstTime && dataRefreshService.getValue().getLastPlayback() > lastUpdated.getTimeInMillis()) {
            if (MediaType.Video.equals(mBaseItem.getMediaType())) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

                        ItemListViewHelperKt.refresh(mItemList);
                        lastUpdated = Calendar.getInstance();

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
        public void onPlaybackStateChange(@NonNull PlaybackController.PlaybackState newState, @Nullable org.jellyfin.sdk.model.api.BaseItemDto currentItem) {
            Timber.i("Got playback state change event %s for item %s", newState.toString(), currentItem != null ? currentItem.getName() : "<unknown>");

            if (newState != PlaybackController.PlaybackState.PLAYING || currentItem == null) {
                if (mCurrentlyPlayingRow != null) mCurrentlyPlayingRow.updateCurrentTime(-1);
                mCurrentlyPlayingRow = mItemList.updatePlaying(null);
            } else {
                mCurrentlyPlayingRow = mItemList.updatePlaying(currentItem.getId().toString());
            }
        }

        @Override
        public void onProgress(long pos) {
            if (mCurrentlyPlayingRow != null) {
                mCurrentlyPlayingRow.updateCurrentTime(pos);
            }
        }

        @Override
        public void onQueueStatusChanged(boolean hasQueue) {}

        @Override
        public void onQueueReplaced() { }
    };

    private void showMenu(final ItemRowView row, boolean showOpen) {
        PopupMenu menu = new PopupMenu(requireContext(), row != null? row : requireActivity().getCurrentFocus(), Gravity.END);
        int order = 0;
        if (showOpen) {
            MenuItem open = menu.getMenu().add(0, 0, order++, R.string.lbl_open);
            open.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    ItemLauncher.launch(new BaseRowItem(row.getItem()), null, 0, requireContext());
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
                play(mItems.subList(row.getIndex(), row.getIndex()+1), false);
                return true;
            }
        });
        if (row.getItem().getType() == BaseItemKind.AUDIO) {
            MenuItem queue = menu.getMenu().add(0, 2, order++, R.string.lbl_add_to_queue);
            queue.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    PlaybackLauncher playbackLauncher = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class);
                    if (playbackLauncher.interceptPlayRequest(requireContext(), row.getItem())) return true;

                    mediaManager.getValue().queueAudioItem(row.getItem());
                    return true;
                }
            });

            MenuItem mix = menu.getMenu().add(0, 1, order++, R.string.lbl_instant_mix);
            mix.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    PlaybackHelper.playInstantMix(requireContext(), row.getItem());
                    return true;
                }
            });

        }

        menu.show();
    }

    private void loadItem(String id) {
        //Special case handling
        if (FakeBaseItem.INSTANCE.getFAV_SONGS().getId().toString().equals(id)) {
            BaseItemDto item = new BaseItemDto();
            item.setId(FakeBaseItem.INSTANCE.getFAV_SONGS_ID().toString());
            item.setName(getString(R.string.lbl_favorites));
            item.setOverview(getString(R.string.desc_automatic_fav_songs));
            item.setPlayAccess(PlayAccess.Full);
            item.setMediaType(MediaType.Audio);
            item.setBaseItemType(BaseItemType.Playlist);
            item.setIsFolder(true);
            setBaseItem(item);
        } else {
            apiClient.getValue().GetItemAsync(id, KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<BaseItemDto>(getLifecycle()) {
                @Override
                public void onResponse(BaseItemDto response) {
                    if (!getActive()) return;

                    setBaseItem(response);
                }
            });
        }
    }

    public void setBaseItem(BaseItemDto item) {
        mBaseItem = item;

        LinearLayout mainInfoRow = (LinearLayout)requireActivity().findViewById(R.id.fdMainInfoRow);

        InfoLayoutHelper.addInfoRow(requireContext(), ModelCompat.asSdk(item), mainInfoRow, false, false);
        addGenres(mGenreRow);
        addButtons(BUTTON_SIZE);
        mSummary.setText(mBaseItem.getOverview());

        updatePoster(mBaseItem);

        //get items
        if (ModelCompat.asSdk(mBaseItem).getType() == BaseItemKind.PLAYLIST) {
            // Have to use different query here
            if (FakeBaseItem.INSTANCE.getFAV_SONGS_ID().toString().equals(mItemId)) {//Get favorited and liked songs from this area
                StdItemQuery favSongs = new StdItemQuery(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.Genres,
                        ItemFields.ChildCount
                });
                favSongs.setParentId(getArguments().getString("ParentId"));
                favSongs.setIncludeItemTypes(new String[]{"Audio"});
                favSongs.setRecursive(true);
                favSongs.setFilters(new ItemFilter[]{ItemFilter.IsFavoriteOrLikes});
                favSongs.setSortBy(new String[]{ItemSortBy.Random});
                favSongs.setLimit(150);
                apiClient.getValue().GetItemsAsync(favSongs, itemResponse);
            } else {
                PlaylistItemQuery playlistSongs = new PlaylistItemQuery();
                playlistSongs.setId(mBaseItem.getId());
                playlistSongs.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                playlistSongs.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.Genres,
                        ItemFields.Chapters,
                        ItemFields.ChildCount
                });
                playlistSongs.setLimit(150);
                apiClient.getValue().GetPlaylistItems(playlistSongs, itemResponse);
            }
        } else {
            StdItemQuery songs = new StdItemQuery();
            songs.setParentId(mBaseItem.getId());
            songs.setRecursive(true);
            songs.setFields(new ItemFields[]{
                    ItemFields.PrimaryImageAspectRatio,
                    ItemFields.Genres,
                    ItemFields.ChildCount
            });
            songs.setIncludeItemTypes(new String[]{"Audio"});
            songs.setSortBy(new String[] {ItemSortBy.SortName});
            songs.setLimit(200);
            apiClient.getValue().GetItemsAsync(songs, itemResponse);
        }
    }

    private LifecycleAwareResponse<ItemsResult> itemResponse = new LifecycleAwareResponse<ItemsResult>(getLifecycle()) {
        @Override
        public void onResponse(ItemsResult response) {
            if (!getActive()) return;

            mTitle.setText(mBaseItem.getName());
            if (mBaseItem.getName().length() > 32) {
                // scale down the title so more will fit
                mTitle.setTextSize(32);
            }
            if (response.getTotalRecordCount() > 0) {
                mItems = new ArrayList<>();
                int i = 0;
                for (BaseItemDto item : response.getItems()) {
                    mItemList.addItem(ModelCompat.asSdk(item), i++);
                    mItems.add(item);
                }
                if (mediaManager.getValue().isPlayingAudio()) {
                    //update our status
                    mAudioEventListener.onPlaybackStateChange(PlaybackController.PlaybackState.PLAYING, mediaManager.getValue().getCurrentAudioItem());
                }

                updateBackdrop();
            }
        }

        @Override
        public void onError(Exception exception) {
            if (!getActive()) return;

            Timber.e(exception, "Error loading");
            Utils.showToast(requireContext(), exception.getLocalizedMessage());
        }
    };

    private void updatePoster(BaseItemDto item){
        if (FakeBaseItem.INSTANCE.getFAV_SONGS_ID().toString().equals(mItemId)) {
            mPoster.setImageResource(R.drawable.favorites);
        } else {
            Double aspect = ImageUtils.getImageAspectRatio(ModelCompat.asSdk(item), false);
            String primaryImageUrl = ImageUtils.getPrimaryImageUrl(ModelCompat.asSdk(item));
            mPoster.setPadding(0, 0, 0, 0);
            mPoster.load(primaryImageUrl, null, ContextCompat.getDrawable(requireContext(), R.drawable.ic_album), aspect, 0);
        }
    }

    private void addGenres(TextView textView) {
        ArrayList<String> genres = mBaseItem.getGenres();
        if (genres != null) textView.setText(TextUtils.join(" / ", genres));
        else textView.setText(null);
    }

    private void play(List<BaseItemDto> items, boolean shuffle) {
        play(items, 0, shuffle);
    }

    private void play(List<BaseItemDto> items, int ndx, boolean shuffle) {
        PlaybackLauncher playbackLauncher = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class);
        if (playbackLauncher.interceptPlayRequest(requireContext(), items.size() > 0 ? ModelCompat.asSdk(items.get(0)) : null)) return;

        Timber.d("play items: %d, ndx: %d, shuffle: %b", items.size(), ndx, shuffle);

        if (MediaType.Video.equals(mBaseItem.getMediaType())) {
            if (shuffle) {
                Collections.shuffle(items);
            }

            int pos = 0;
            BaseItemDto item = items.size() > 0 ? items.get(ndx) : null;
            if (item != null && item.getUserData() != null) {
                pos = Math.toIntExact(item.getUserData().getPlaybackPositionTicks() / 10000);
            }
            videoQueueManager.getValue().setCurrentVideoQueue(JavaCompat.mapBaseItemCollection(items));
            videoQueueManager.getValue().setCurrentMediaPosition(ndx);
            Destination destination = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackDestination(ModelCompat.asSdk(mBaseItem).getType(), pos);
            navigationRepository.getValue().navigate(destination);
        } else {
            mediaManager.getValue().playNow(requireContext(), JavaCompat.mapBaseItemCollection(items), ndx, shuffle);
        }
    }

    private void addButtons(int buttonSize) {
        if (BaseItemExtensionsKt.canPlay(ModelCompat.asSdk(mBaseItem))) {
            // add play button but don't show and focus yet
            TextUnderButton play = TextUnderButton.create(requireContext(), R.drawable.ic_play, buttonSize, 2, getString(mBaseItem.getIsFolderItem() ? R.string.lbl_play_all : R.string.lbl_play), new View.OnClickListener() {
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
            if (ModelCompat.asSdk(mBaseItem).getType() == BaseItemKind.MUSIC_ALBUM && mediaManager.getValue().hasAudioQueueItems()) {
                queueButton = TextUnderButton.create(requireContext(), R.drawable.ic_add, buttonSize, 2, getString(R.string.lbl_add_to_queue), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mediaManager.getValue().addToAudioQueue(JavaCompat.mapBaseItemCollection(mItems));
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

            if (mBaseItem.getIsFolderItem()) {
                TextUnderButton shuffle = TextUnderButton.create(requireContext(), R.drawable.ic_shuffle, buttonSize, 2, getString(R.string.lbl_shuffle_all), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mItems.size() > 0) {
                            //use server retrieval in order to get all items
                            PlaybackHelper.retrieveAndPlay(mBaseItem.getId(), true, requireContext());
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

        if (ModelCompat.asSdk(mBaseItem).getType() == BaseItemKind.MUSIC_ALBUM) {
            TextUnderButton mix = TextUnderButton.create(requireContext(), R.drawable.ic_mix, buttonSize, 2, getString(R.string.lbl_instant_mix), new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    PlaybackHelper.playInstantMix(requireContext(), ModelCompat.asSdk(mBaseItem));
                }
            });
            mButtonRow.addView(mix);
            mix.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) mScrollView.smoothScrollTo(0, 0);
            });
        }

        if (!FakeBaseItem.INSTANCE.getFAV_SONGS_ID().toString().equals(mItemId)) {
            //Favorite
            TextUnderButton fav = TextUnderButton.create(requireContext(), R.drawable.ic_heart, buttonSize,2, getString(R.string.lbl_favorite), new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    UserItemDataDto data = mBaseItem.getUserData();
                    apiClient.getValue().UpdateFavoriteStatusAsync(mBaseItem.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), !data.getIsFavorite(), new LifecycleAwareResponse<UserItemDataDto>(getLifecycle()) {
                        @Override
                        public void onResponse(UserItemDataDto response) {
                            if (!getActive()) return;

                            mBaseItem.setUserData(response);
                            ((TextUnderButton)v).setActivated(response.getIsFavorite());
                            dataRefreshService.getValue().setLastFavoriteUpdate(System.currentTimeMillis());
                        }
                    });
                }
            });
            fav.setActivated(mBaseItem.getUserData().getIsFavorite());
            mButtonRow.addView(fav);
            fav.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) mScrollView.smoothScrollTo(0, 0);
            });
        }

        if (mBaseItem.getAlbumArtists() != null && mBaseItem.getAlbumArtists().size() > 0) {
            TextUnderButton artist = TextUnderButton.create(requireContext(), R.drawable.ic_user, buttonSize, 4, getString(R.string.lbl_open_artist), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigationRepository.getValue().navigate(Destinations.INSTANCE.itemDetails(UUIDSerializerKt.toUUID(mBaseItem.getAlbumArtists().get(0).getId())));
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

        if(item.getBackdropCount() == 0 && mItems != null && mItems.size() >= 1)
            item = mItems.get(new Random().nextInt(mItems.size()));

        backgroundService.getValue().setBackground(ModelCompat.asSdk(item));
    }
}
