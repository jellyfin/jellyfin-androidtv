package org.jellyfin.androidtv.ui.itemdetail;

import static org.koin.java.KoinJavaComponent.inject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.model.GotFocusEvent;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.databinding.ActivityItemListBinding;
import org.jellyfin.androidtv.databinding.ViewRowDetailsBinding;
import org.jellyfin.androidtv.ui.ItemListView;
import org.jellyfin.androidtv.ui.ItemRowView;
import org.jellyfin.androidtv.ui.TextUnderButton;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.playback.AudioEventListener;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.MathUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.BaseItemUtils;
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.library.PlayAccess;
import org.jellyfin.apiclient.model.playlists.PlaylistItemQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemFilter;
import org.jellyfin.apiclient.model.querying.ItemSortBy;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

public class ItemListActivity extends FragmentActivity {
    private int BUTTON_SIZE;
    public static final String FAV_SONGS = "FAV_SONGS";
    public static final String VIDEO_QUEUE = "VIDEO_QUEUE";

    private TextView mTitle;
    private TextView mGenreRow;
    private ImageView mPoster;
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

    private Activity mActivity;
    private DisplayMetrics mMetrics;

    private boolean firstTime = true;
    private Calendar lastUpdated = Calendar.getInstance();

    private final Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private final Lazy<DataRefreshService> dataRefreshService = inject(DataRefreshService.class);
    private Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private Lazy<MediaManager> mediaManager = inject(MediaManager.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityItemListBinding binding = ActivityItemListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mActivity = this;
        BUTTON_SIZE = Utils.convertDpToPixel(this, 35);

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
        params.width = Utils.convertDpToPixel(this,100);


        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
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
                //TvApp.getApplication().getLogger().Debug("Row selected: "+row.getItem().getName()+" at "+location[1]+" Screen edge: "+mMetrics.heightPixels);
            }
        });

        mItemList.setRowClickedListener(new ItemRowView.RowClickedListener() {
            @Override
            public void onRowClicked(ItemRowView row) {
                showMenu(row, row.getItem().getBaseItemType() != BaseItemType.Audio);
            }
        });

        backgroundService.getValue().attach(this);

        mItemId = getIntent().getStringExtra("ItemId");
        loadItem(mItemId);

    }



    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
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

        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaManager.getValue().addAudioEventListener(mAudioEventListener);
        // and fire it to be sure we're updated
        mAudioEventListener.onPlaybackStateChange(mediaManager.getValue().isPlayingAudio() ? PlaybackController.PlaybackState.PLAYING : PlaybackController.PlaybackState.IDLE, mediaManager.getValue().getCurrentAudioItem());

        if (!firstTime && dataRefreshService.getValue().getLastPlayback() > lastUpdated.getTimeInMillis()) {
            if (mItemId.equals(VIDEO_QUEUE)) {
                //update this in case it changed - delay to allow for the changes
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mItems = mediaManager.getValue().getCurrentVideoQueue();
                        if (mItems != null && mItems.size() > 0) {
                            mItemList.clear();
                            mCurrentRow = null;
                            mItemList.addItems(mItems);
                            lastUpdated = Calendar.getInstance();
                        } else {
                            //nothing left in queue
                            finish();
                        }
                    }
                }, 750);
            } else if ("Video".equals(mBaseItem.getMediaType())) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mItemList.refresh();
                        lastUpdated = Calendar.getInstance();

                    }
                }, 500);
            }
        }

        firstTime = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mediaManager.getValue().removeAudioEventListener(mAudioEventListener);
    }

    private AudioEventListener mAudioEventListener = new AudioEventListener() {
        @Override
        public void onPlaybackStateChange(PlaybackController.PlaybackState newState, BaseItemDto currentItem) {
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
    };

    private GotFocusEvent mainAreaFocusListener = new GotFocusEvent() {
        @Override
        public void gotFocus(View v) {
            //scroll so entire main area is in view
            mScrollView.smoothScrollTo(0, 0);
        }
    };

    private void showMenu(final ItemRowView row, boolean showOpen) {
        PopupMenu menu = new PopupMenu(this, row != null? row : getCurrentFocus(), Gravity.END);
        int order = 0;
        if (showOpen) {
            MenuItem open = menu.getMenu().add(0, 0, order++, R.string.lbl_open);
            open.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    ItemLauncher.launch(new BaseRowItem(0, row.getItem()), null, 0, mActivity);
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
        MenuItem queue = menu.getMenu().add(0, 2, order++, R.string.lbl_add_to_queue);
        queue.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (row.getItem().getMediaType()) {
                    case "Video":
                        mediaManager.getValue().addToVideoQueue(row.getItem());
                        break;
                    case "Audio":
                        mediaManager.getValue().queueAudioItem(row.getItem());
                        break;
                }
                return true;
            }
        });
        if (row.getItem().getBaseItemType() == BaseItemType.Audio) {
            MenuItem mix = menu.getMenu().add(0, 1, order++, R.string.lbl_instant_mix);
            mix.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    PlaybackHelper.playInstantMix(ItemListActivity.this, row.getItem());
                    return true;
                }
            });

        }

        menu.show();
    }

    private void loadItem(String id) {
        //Special case handling
        switch (id) {
            case FAV_SONGS:
                BaseItemDto item = new BaseItemDto();
                item.setId(FAV_SONGS);
                item.setName(getString(R.string.lbl_favorites));
                item.setOverview(getString(R.string.desc_automatic_fav_songs));
                item.setPlayAccess(PlayAccess.Full);
                item.setMediaType("Audio");
                item.setBaseItemType(BaseItemType.Playlist);
                item.setIsFolder(true);
                setBaseItem(item);
                break;
            case VIDEO_QUEUE:
                BaseItemDto queue = new BaseItemDto();
                queue.setId(VIDEO_QUEUE);
                queue.setName(getString(R.string.lbl_current_queue));
                queue.setOverview(getString(R.string.desc_current_video_queue));
                queue.setPlayAccess(PlayAccess.Full);
                queue.setMediaType("Video");
                queue.setBaseItemType(BaseItemType.Playlist);
                queue.setIsFolder(true);
                if (mediaManager.getValue().getCurrentVideoQueue() != null) {
                    long runtime = 0;
                    int children = 0;
                    for (BaseItemDto video : mediaManager.getValue().getCurrentVideoQueue()) {
                        runtime += video.getRunTimeTicks() != null ? video.getRunTimeTicks() : 0;
                        children += 1;
                    }
                    queue.setCumulativeRunTimeTicks(runtime);
                    queue.setChildCount(children);
                }
                setBaseItem(queue);
                break;
            default:
                apiClient.getValue().GetItemAsync(id, TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        setBaseItem(response);
                    }
                });
                break;
        }
    }

    public void setBaseItem(BaseItemDto item) {
        mBaseItem = item;

        LinearLayout mainInfoRow = (LinearLayout)findViewById(R.id.fdMainInfoRow);

        InfoLayoutHelper.addInfoRow(this, item, mainInfoRow, false, false);
        addGenres(mGenreRow);
        addButtons(BUTTON_SIZE);
        mSummary.setText(mBaseItem.getOverview());

        updatePoster(mBaseItem);

        //get items
        if (mBaseItem.getBaseItemType() == BaseItemType.Playlist) {
            // Have to use different query here
            switch (mItemId) {
                case FAV_SONGS:
                    //Get favorited and liked songs from this area
                    StdItemQuery favSongs = new StdItemQuery(new ItemFields[] {
                            ItemFields.PrimaryImageAspectRatio,
                            ItemFields.Genres,
                            ItemFields.ChildCount
                    });
                    favSongs.setParentId(getIntent().getStringExtra("ParentId"));
                    favSongs.setIncludeItemTypes(new String[] {"Audio"});
                    favSongs.setRecursive(true);
                    favSongs.setFilters(new ItemFilter[]{ItemFilter.IsFavoriteOrLikes});
                    favSongs.setSortBy(new String[]{ItemSortBy.Random});
                    favSongs.setLimit(150);
                    apiClient.getValue().GetItemsAsync(favSongs, itemResponse);
                    break;
                case VIDEO_QUEUE:
                    //Show current queue
                    mTitle.setText(mBaseItem.getName());
                    mItemList.addItems(mediaManager.getValue().getCurrentVideoQueue());
                    mItems.addAll(mediaManager.getValue().getCurrentVideoQueue());
                    updateBackdrop();
                    break;
                default:
                    PlaylistItemQuery playlistSongs = new PlaylistItemQuery();
                    playlistSongs.setId(mBaseItem.getId());
                    playlistSongs.setUserId(TvApp.getApplication().getCurrentUser().getId());
                    playlistSongs.setFields(new ItemFields[]{
                            ItemFields.PrimaryImageAspectRatio,
                            ItemFields.Genres,
                            ItemFields.Chapters,
                            ItemFields.ChildCount
                    });
                    playlistSongs.setLimit(150);
                    apiClient.getValue().GetPlaylistItems(playlistSongs, itemResponse);
                    break;
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

    private Response<ItemsResult> itemResponse = new Response<ItemsResult>() {
        @Override
        public void onResponse(ItemsResult response) {
            mTitle.setText(mBaseItem.getName());
            if (mBaseItem.getName().length() > 32) {
                // scale down the title so more will fit
                mTitle.setTextSize(32);
            }
            if (response.getTotalRecordCount() > 0) {
                mItems = new ArrayList<>();
                int i = 0;
                for (BaseItemDto item : response.getItems()) {
                    mItemList.addItem(item, i++);
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
            Timber.e(exception, "Error loading");
            Utils.showToast(mActivity, exception.getLocalizedMessage());
        }
    };

    private void updatePoster(BaseItemDto item){
        switch (mItemId) {
            case FAV_SONGS:
                mPoster.setImageResource(R.drawable.favorites);
                break;
            case VIDEO_QUEUE:
                mPoster.setImageResource(R.drawable.ic_video_queue);
                break;
            default:
                // Figure image size
                Double aspect = ImageUtils.getImageAspectRatio(item, false);
                int posterHeight = aspect > 1 ? Utils.convertDpToPixel(this, 160) : Utils.convertDpToPixel(this, 250);
                int posterWidth = (int)((aspect) * posterHeight);
                if (posterHeight < 10) posterWidth = Utils.convertDpToPixel(this, 150);  //Guard against zero size images causing picasso to barf

                String primaryImageUrl = ImageUtils.getPrimaryImageUrl(this, mBaseItem, false, posterHeight);


                Glide.with(this)
                        .load(primaryImageUrl)
                        .override(posterWidth,posterHeight)
                        .fitCenter()
                        .into(mPoster);

                break;
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
        if (playbackLauncher.interceptPlayRequest(this, items.size() > 0 ? items.get(0) : null)) return;

        if ("Video".equals(mBaseItem.getMediaType())) {
            Class activity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(mBaseItem.getBaseItemType());
            Intent intent = new Intent(mActivity, activity);
            //Resume first item if needed
            BaseItemDto first = items.size() > 0 ? items.get(0) : null;
            if (first != null && first.getUserData() != null) {
                Long pos = first.getUserData().getPlaybackPositionTicks() / 10000;
                intent.putExtra("Position", pos.intValue());
            }
            mediaManager.getValue().setCurrentVideoQueue(items);
            startActivity(intent);
        } else {
            mediaManager.getValue().playNow(this, items, ndx, shuffle);
        }
    }

    private void addButtons(int buttonSize) {
        if (BaseItemUtils.canPlay(mBaseItem)) {
            // add play button but don't show and focus yet
            TextUnderButton play = TextUnderButton.create(this, R.drawable.ic_play, buttonSize, 2, getString(mBaseItem.getIsFolderItem() ? R.string.lbl_play_all : R.string.lbl_play), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItems.size() > 0) {
                        play(mItems, false);
                    } else {
                        Utils.showToast(mActivity, R.string.msg_no_playable_items);
                    }
                }
            });
            play.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) mainAreaFocusListener.gotFocus(v);
            });
            mButtonRow.addView(play);

            boolean hidePlayButton = false;
            TextUnderButton queueButton = null;
            // add to queue if a queue exists and mBaseItem is a MusicAlbum
            if (mBaseItem.getBaseItemType() == BaseItemType.MusicAlbum && mediaManager.getValue().hasAudioQueueItems()) {
                queueButton = TextUnderButton.create(this, R.drawable.ic_add, buttonSize, 2, getString(R.string.lbl_add_to_queue), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mediaManager.getValue().addToAudioQueue(mItems);
                    }
                });
                hidePlayButton = true;
                mButtonRow.addView(queueButton);
                queueButton.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) mainAreaFocusListener.gotFocus(v);
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
                TextUnderButton shuffle = TextUnderButton.create(this, R.drawable.ic_shuffle, buttonSize, 2, getString(R.string.lbl_shuffle_all), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mItems.size() > 0) {
                            if (mBaseItem.getId().equals(VIDEO_QUEUE)
                                    || mBaseItem.getId().equals(FAV_SONGS)
                                    || mBaseItem.getBaseItemType() == BaseItemType.Playlist
                                    || mBaseItem.getBaseItemType() == BaseItemType.MusicAlbum) {
                                play(mItems, true);
                            } else {
                                //use server retrieval in order to get all items
                                PlaybackHelper.retrieveAndPlay(mBaseItem.getId(), true, mActivity);
                            }
                        } else {
                            Utils.showToast(mActivity, R.string.msg_no_playable_items);
                        }
                    }
                });
                mButtonRow.addView(shuffle);
                shuffle.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) mainAreaFocusListener.gotFocus(v);
                });
            }
        }

        if (mBaseItem.getBaseItemType() == BaseItemType.MusicAlbum) {
            TextUnderButton mix = TextUnderButton.create(this, R.drawable.ic_mix, buttonSize, 2, getString(R.string.lbl_instant_mix), new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    PlaybackHelper.playInstantMix(ItemListActivity.this, mBaseItem);
                }
            });
            mButtonRow.addView(mix);
            mix.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) mainAreaFocusListener.gotFocus(v);
            });
        }

        if (!mItemId.equals(FAV_SONGS)) {
            if (!mItemId.equals(VIDEO_QUEUE)) {
                //Favorite
                TextUnderButton fav = TextUnderButton.create(this, R.drawable.ic_heart, buttonSize,2, getString(R.string.lbl_favorite), new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        UserItemDataDto data = mBaseItem.getUserData();
                        apiClient.getValue().UpdateFavoriteStatusAsync(mBaseItem.getId(), TvApp.getApplication().getCurrentUser().getId(), !data.getIsFavorite(), new Response<UserItemDataDto>() {
                            @Override
                            public void onResponse(UserItemDataDto response) {
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
                    if (hasFocus) mainAreaFocusListener.gotFocus(v);
                });
            }

            if (mBaseItem.getBaseItemType() == BaseItemType.Playlist) {
                if (VIDEO_QUEUE.equals(mBaseItem.getId())) {
                    mButtonRow.addView(TextUnderButton.create(this, R.drawable.ic_save, buttonSize, 2, getString(R.string.lbl_save_as_playlist), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mediaManager.getValue().saveVideoQueue(mActivity);
                        }
                    }));
                }

                TextUnderButton delete = TextUnderButton.create(this, R.drawable.ic_trash, buttonSize, 0, getString(R.string.lbl_delete), new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        if (mBaseItem.getId().equals(VIDEO_QUEUE)) {
                            new AlertDialog.Builder(mActivity)
                                    .setTitle(R.string.lbl_clear_queue)
                                    .setMessage(R.string.clear_expanded)
                                    .setPositiveButton(R.string.lbl_clear, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            mediaManager.getValue().setCurrentVideoQueue(new ArrayList<BaseItemDto>());
                                            dataRefreshService.getValue().setLastVideoQueueChange(System.currentTimeMillis());
                                            finish();
                                        }
                                    })
                                    .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                                    .show();

                        } else {
                            new AlertDialog.Builder(mActivity)
                                    .setTitle(R.string.lbl_delete)
                                    .setMessage(getString(R.string.delete_warning, mBaseItem.getName()))
                                    .setPositiveButton(R.string.lbl_delete, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            apiClient.getValue().DeleteItem(mBaseItem.getId(), new EmptyResponse() {
                                                @Override
                                                public void onResponse() {
                                                    Utils.showToast(mActivity, getString(R.string.lbl_deleted, mBaseItem.getName()));
                                                    dataRefreshService.getValue().setLastDeletedItemId(mBaseItem.getId());
                                                    finish();
                                                }

                                                @Override
                                                public void onError(Exception ex) {
                                                    Utils.showToast(mActivity, ex.getLocalizedMessage());
                                                }
                                            });
                                        }
                                    })
                                    .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Utils.showToast(mActivity, R.string.not_deleted);
                                        }
                                    })
                                    .show();


                        }
                    }
                });

                mButtonRow.addView(delete);
                delete.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) mainAreaFocusListener.gotFocus(v);
                });
            }
        }

        if (mBaseItem.getAlbumArtists() != null && mBaseItem.getAlbumArtists().size() > 0) {
            TextUnderButton artist = TextUnderButton.create(this, R.drawable.ic_user, buttonSize, 4, getString(R.string.lbl_open_artist), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent artist = new Intent(mActivity, FullDetailsActivity.class);
                    artist.putExtra("ItemId", mBaseItem.getAlbumArtists().get(0).getId());
                    mActivity.startActivity(artist);

                }
            });
            mButtonRow.addView(artist);
            artist.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) mainAreaFocusListener.gotFocus(v);
            });
        }

    }

    private void updateBackdrop() {
        BaseItemDto item = mBaseItem;

        if(item.getBackdropCount() == 0 && mItems != null && mItems.size() >= 1)
            item = mItems.get(MathUtils.randInt(0, mItems.size() - 1));

        backgroundService.getValue().setBackground(item);
    }
}
