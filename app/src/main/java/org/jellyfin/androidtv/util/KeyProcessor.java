package org.jellyfin.androidtv.util;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.PopupMenu;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.ui.itemdetail.ItemListActivity;
import org.jellyfin.androidtv.ui.itemdetail.PhotoPlayerActivity;
import org.jellyfin.androidtv.ui.itemhandling.AudioQueueItem;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.playback.AudioNowPlayingActivity;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.shared.BaseActivity;
import org.jellyfin.androidtv.util.apiclient.BaseItemUtils;
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.entities.SortOrder;
import org.jellyfin.apiclient.model.querying.ItemFilter;
import org.jellyfin.apiclient.model.querying.ItemsResult;

import java.util.List;

import timber.log.Timber;

import static org.koin.java.KoinJavaComponent.get;

public class KeyProcessor {

    public static final int MENU_MARK_FAVORITE = 0;
    public static final int MENU_UNMARK_FAVORITE = 1;
    public static final int MENU_LIKE = 2;
    public static final int MENU_DISLIKE = 3;
    public static final int MENU_UNLIKE = 4;
    public static final int MENU_UNDISLIKE = 5;
    public static final int MENU_MARK_PLAYED = 6;
    public static final int MENU_UNMARK_PLAYED = 7;
    public static final int MENU_PLAY = 8;
    public static final int MENU_PLAY_SHUFFLE = 9;
    public static final int MENU_PLAY_FIRST_UNWATCHED = 10;
    public static final int MENU_ADD_QUEUE = 11;
    public static final int MENU_ADVANCE_QUEUE = 12;
    public static final int MENU_REMOVE_FROM_QUEUE = 13;
    public static final int MENU_GOTO_NOW_PLAYING = 14;
    public static final int MENU_INSTANT_MIX = 15;
    public static final int MENU_FORGET = 16;

    private static String mCurrentItemId;
    private static BaseItemDto mCurrentItem;
    private static Activity mCurrentActivity;
    private static int mCurrentRowItemNdx;
    private static boolean currentItemIsFolder = false;
    private static boolean isMusic;

    public static boolean HandleKey(int key, BaseRowItem rowItem, Activity activity) {
        if (rowItem == null) return false;
        switch (key) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (get(MediaManager.class).isPlayingAudio() && (!rowItem.isBaseItem() || rowItem.getBaseItemType() != BaseItemType.Photo)) {
                    get(MediaManager.class).pauseAudio();
                    return true;
                }

                switch (rowItem.getItemType()) {

                    case BaseItem:
                        BaseItemDto item = rowItem.getBaseItem();
                        if (!BaseItemUtils.canPlay(item)) return false;
                        switch (item.getBaseItemType()) {
                            case Audio:
                                if (rowItem instanceof AudioQueueItem) {
                                    createItemMenu(rowItem, item.getUserData(), activity);
                                    return true;
                                }
                                //fall through...
                            case Movie:
                            case Episode:
                            case TvChannel:
                            case Video:
                            case Program:
                            case ChannelVideoItem:
                            case Trailer:
                                // retrieve full item and play
                                PlaybackHelper.retrieveAndPlay(item.getId(), false, activity);
                                return true;
                            case Series:
                            case Season:
                            case BoxSet:
                                createPlayMenu(rowItem.getBaseItem(), true, false, activity);
                                return true;
                            case MusicAlbum:
                            case MusicArtist:
                                createPlayMenu(rowItem.getBaseItem(), true, true, activity);
                                return true;
                            case Playlist:
                                createPlayMenu(rowItem.getBaseItem(), true, "Audio".equals(rowItem.getBaseItem().getMediaType()), activity);
                                return true;
                            case Photo:
                                // open photo player
                                Intent photoIntent = new Intent(activity, PhotoPlayerActivity.class);
                                photoIntent.putExtra("Play",true);
                                activity.startActivity(photoIntent);
                                return true;
                        }
                        break;
                    case Person:
                        break;
                    case Server:
                        break;
                    case User:
                        break;
                    case Chapter:
                        break;
                    case SearchHint:
                        switch (rowItem.getSearchHint().getType()) {
                            case "Movie":
                            case "Episode":
                            case "TvChannel":
                            case "Video":
                            case "Program":
                                // retrieve full item and play
                                PlaybackHelper.retrieveAndPlay(rowItem.getItemId(), false, activity);
                                return true;
                            case "Series":
                            case "Season":
                            case "BoxSet":
                                createPlayMenu(rowItem.getBaseItem(), true, false, activity);
                                return true;
                        }
                        break;
                    case LiveTvChannel:
                    case LiveTvRecording:
                        // retrieve full item and play
                        PlaybackHelper.retrieveAndPlay(rowItem.getItemId(), false, activity);
                        return true;
                    case LiveTvProgram:
                        // retrieve channel this program belongs to and play
                        PlaybackHelper.retrieveAndPlay(rowItem.getProgramInfo().getChannelId(), false, activity);
                        return true;
                    case GridButton:
                        if (rowItem.getGridButton().getId() == TvApp.VIDEO_QUEUE_OPTION_ID) {
                            //Queue already there - just kick off playback
                            BaseItemType itemType = get(MediaManager.class).getCurrentVideoQueue().size() > 0 ? get(MediaManager.class).getCurrentVideoQueue().get(0).getBaseItemType() : null;
                            Intent intent = new Intent(activity, TvApp.getApplication().getPlaybackActivityClass(itemType));
                            activity.startActivity(intent);
                        }
                        break;
                }

                if (get(MediaManager.class).hasAudioQueueItems()) {
                    get(MediaManager.class).resumeAudio();
                    return true;
                }

                break;
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_BUTTON_Y:
                Timber.d("Menu for: %s", rowItem.getFullName());

                //Create a contextual menu based on item
                switch (rowItem.getItemType()) {

                    case BaseItem:
                        BaseItemDto item = rowItem.getBaseItem();
                        switch (item.getBaseItemType()) {
                            case Movie:
                            case Episode:
                            case TvChannel:
                            case Video:
                            case Program:
                            case ChannelVideoItem:
                            case Series:
                            case Season:
                            case BoxSet:
                            case MusicAlbum:
                            case MusicArtist:
                            case Playlist:
                            case Audio:
                            case Trailer:
                                // generate a standard item menu
                                createItemMenu(rowItem, item.getUserData(), activity);
                                break;
                        }
                        break;
                    case Person:
                        break;
                    case Server:
                        break;
                    case User:
                        break;
                    case Chapter:
                        break;
                    case SearchHint:
                        break;
                    case LiveTvChannel:
                        break;
                    case LiveTvRecording:
                        break;
                    case GridButton:
                        break;
                    case LiveTvProgram:
                        break;
                }
                return true;
        }
        return false;
    }

    private static void createItemMenu(BaseRowItem rowItem, UserItemDataDto userData, Activity activity) {
        BaseItemDto item = rowItem.getBaseItem();
        PopupMenu menu = Utils.createPopupMenu(activity, activity.getCurrentFocus(), Gravity.RIGHT);
        int order = 0;

        if (rowItem instanceof AudioQueueItem) {
            if (!(activity instanceof AudioNowPlayingActivity)) {
                menu.getMenu().add(0, MENU_GOTO_NOW_PLAYING, order++, R.string.lbl_goto_now_playing);
            }
            if (rowItem.getBaseItem() != get(MediaManager.class).getCurrentAudioItem()) {
                menu.getMenu().add(0, MENU_ADVANCE_QUEUE, order++, R.string.lbl_play_from_here);
            }
            // don't allow removal of last item - framework will crash trying to animate an empty row
            if (get(MediaManager.class).getCurrentAudioQueue().size() > 1) {
                menu.getMenu().add(0, MENU_REMOVE_FROM_QUEUE, order++, R.string.lbl_remove_from_queue);
            }
        } else {
            if (BaseItemUtils.canPlay(item)) {
                if (item.getIsFolderItem()
                        && item.getBaseItemType() != BaseItemType.MusicAlbum
                        && item.getBaseItemType() != BaseItemType.Playlist
                        && item.getBaseItemType() != BaseItemType.MusicArtist
                        && userData!= null
                        && userData.getUnplayedItemCount() !=null
                        && userData.getUnplayedItemCount() > 0) {
                    menu.getMenu().add(0, MENU_PLAY_FIRST_UNWATCHED, order++, R.string.lbl_play_first_unwatched);
                }
                menu.getMenu().add(0, MENU_PLAY, order++, item.getIsFolderItem() ? R.string.lbl_play_all : R.string.lbl_play);
                if (item.getIsFolderItem()) {
                    menu.getMenu().add(0, MENU_PLAY_SHUFFLE, order++, R.string.lbl_shuffle_all);
                }
            }

            isMusic = item.getBaseItemType() == BaseItemType.MusicAlbum
                    || item.getBaseItemType() == BaseItemType.MusicArtist
                    || item.getBaseItemType() == BaseItemType.Audio
                    || (item.getBaseItemType() == BaseItemType.Playlist && "Audio".equals(item.getMediaType()));

            if (isMusic || !item.getIsFolderItem()) {
                menu.getMenu().add(0, MENU_ADD_QUEUE, order++, R.string.lbl_add_to_queue);
            }

            if (isMusic) {
                if (item.getBaseItemType() != BaseItemType.Playlist) {
                    menu.getMenu().add(0, MENU_INSTANT_MIX, order++, R.string.lbl_instant_mix);
                }
            } else {
                if (userData != null && userData.getPlayed()) {
                    menu.getMenu().add(0, MENU_UNMARK_PLAYED, order++, activity.getString(R.string.lbl_mark_unplayed));
                } else {
                    menu.getMenu().add(0, MENU_MARK_PLAYED, order++, activity.getString(R.string.lbl_mark_played));
                }
            }
        }

        if (userData != null) {
            if (userData.getIsFavorite()) {
                menu.getMenu().add(0, MENU_UNMARK_FAVORITE, order++, activity.getString(R.string.lbl_remove_favorite));
            } else {
                menu.getMenu().add(0, MENU_MARK_FAVORITE, order++, activity.getString(R.string.lbl_add_favorite));
            }

            if (userData.getLikes() == null) {
                menu.getMenu().add(0, MENU_LIKE, order++, activity.getString(R.string.lbl_like));
                menu.getMenu().add(0, MENU_DISLIKE, order++, activity.getString(R.string.lbl_dislike));
            } else if (userData.getLikes()) {
                menu.getMenu().add(0, MENU_UNLIKE, order++, activity.getString(R.string.lbl_unlike));
                menu.getMenu().add(0, MENU_DISLIKE, order++, activity.getString(R.string.lbl_dislike));
            } else {
                menu.getMenu().add(0, MENU_LIKE, order++, activity.getString(R.string.lbl_like));
                menu.getMenu().add(0, MENU_UNDISLIKE, order++, activity.getString(R.string.lbl_remove_dislike));
            }
        }

        //Not sure I like this but I either duplicate processing with in-line events or do this and
        // use a single event handler
        mCurrentItem = item;
        mCurrentRowItemNdx = rowItem.getIndex();
        mCurrentItemId = item.getId();
        mCurrentActivity = activity;
        currentItemIsFolder = item.getIsFolderItem();

        menu.setOnMenuItemClickListener(menuItemClickListener);
        menu.show();
    }

    private static void createPlayMenu(BaseItemDto item, boolean isFolder, boolean isMusic, Activity activity) {
        PopupMenu menu = Utils.createPopupMenu(activity, activity.getCurrentFocus(), Gravity.RIGHT);
        int order = 0;
        if (!isMusic && item.getBaseItemType() != BaseItemType.Playlist) {
            menu.getMenu().add(0, MENU_PLAY_FIRST_UNWATCHED, order++, R.string.lbl_play_first_unwatched);
        }
        menu.getMenu().add(0, MENU_PLAY, order++, R.string.lbl_play_all);
        menu.getMenu().add(0, MENU_PLAY_SHUFFLE, order++, R.string.lbl_shuffle_all);
        if (isMusic) {
            menu.getMenu().add(0, MENU_ADD_QUEUE, order, R.string.lbl_add_to_queue);
        }

        //Not sure I like this but I either duplicate processing with in-line events or do this and
        // use a single event handler
        mCurrentItem = item;
        mCurrentItemId = item.getId();
        mCurrentActivity = activity;
        currentItemIsFolder = isFolder;

        menu.setOnMenuItemClickListener(menuItemClickListener);
        menu.show();
    }

    private static PopupMenu.OnMenuItemClickListener menuItemClickListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {

            switch (item.getItemId()) {
                case MENU_PLAY:
                    if (mCurrentItemId.equals(ItemListActivity.FAV_SONGS)) {
                        PlaybackHelper.play(mCurrentItem, 0, false, mCurrentActivity);
                    } else {
                        PlaybackHelper.retrieveAndPlay(mCurrentItemId, false, mCurrentActivity);
                    }
                    return true;
                case MENU_PLAY_SHUFFLE:
                    if (mCurrentItemId.equals(ItemListActivity.FAV_SONGS)) {
                        PlaybackHelper.play(mCurrentItem, 0, false, mCurrentActivity);
                    } else {
                        PlaybackHelper.retrieveAndPlay(mCurrentItemId, true, mCurrentActivity);
                    }
                    return true;
                case MENU_ADD_QUEUE:
                    if (isMusic) {
                        PlaybackHelper.getItemsToPlay(mCurrentItem, false, false, new Response<List<BaseItemDto>>() {
                            @Override
                            public void onResponse(List<BaseItemDto> response) {
                                get(MediaManager.class).addToAudioQueue(response);
                            }

                            @Override
                            public void onError(Exception exception) {
                                Utils.showToast(mCurrentActivity, R.string.msg_cannot_play_time);
                            }
                        });

                    } else {
                        get(ApiClient.class).GetItemAsync(mCurrentItemId, TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                            @Override
                            public void onResponse(BaseItemDto response) {
                                get(MediaManager.class).addToVideoQueue(response);
                            }

                            @Override
                            public void onError(Exception exception) {
                                Utils.showToast(mCurrentActivity, R.string.msg_cannot_play_time);
                            }
                        });
                    }
                    return true;
                case MENU_PLAY_FIRST_UNWATCHED:
                    StdItemQuery query = new StdItemQuery();
                    query.setParentId(mCurrentItemId);
                    query.setRecursive(true);
                    query.setIsVirtualUnaired(false);
                    query.setIsMissing(false);
                    query.setSortBy(new String[]{"SortName"});
                    query.setSortOrder(SortOrder.Ascending);
                    query.setLimit(1);
                    query.setExcludeItemTypes(new String[] {"Series","Season","Folder","MusicAlbum","Playlist","BoxSet"});
                    query.setFilters(new ItemFilter[] {ItemFilter.IsUnplayed});
                    get(ApiClient.class).GetItemsAsync(query, new Response<ItemsResult>() {
                        @Override
                        public void onResponse(ItemsResult response) {
                            if (response.getTotalRecordCount() == 0) {
                                Utils.showToast(mCurrentActivity, R.string.msg_no_items);
                            } else {
                                PlaybackHelper.retrieveAndPlay(response.getItems()[0].getId(), false, mCurrentActivity);
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            Timber.e(exception, "Error trying to play first unwatched");
                            Utils.showToast(mCurrentActivity, R.string.msg_video_playback_error);
                        }
                    });
                    return true;
                case MENU_MARK_FAVORITE:
                    toggleFavorite(true);
                    return true;
                case MENU_UNMARK_FAVORITE:
                    toggleFavorite(false);
                    return true;
                case MENU_MARK_PLAYED:
                    markPlayed();
                    return true;
                case MENU_UNMARK_PLAYED:
                    markUnplayed();
                    return true;
                case MENU_LIKE:
                    toggleLikes(true);
                    return true;
                case MENU_DISLIKE:
                    toggleLikes(false);
                    return true;
                case MENU_UNLIKE:
                case MENU_UNDISLIKE:
                    toggleLikes(null);
                    return true;
                case MENU_GOTO_NOW_PLAYING:
                    Intent nowPlaying = new Intent(mCurrentActivity, AudioNowPlayingActivity.class);
                    mCurrentActivity.startActivity(nowPlaying);
                    return true;
                case MENU_REMOVE_FROM_QUEUE:
                    get(MediaManager.class).removeFromAudioQueue(mCurrentRowItemNdx);
                    return true;
                case MENU_ADVANCE_QUEUE:
                    get(MediaManager.class).playFrom(mCurrentRowItemNdx);
                    return true;
                case MENU_INSTANT_MIX:
                    PlaybackHelper.playInstantMix(mCurrentItemId);
                    return true;
            }

            return false;
            }
    };

    private static void markPlayed() {
        get(ApiClient.class).MarkPlayedAsync(mCurrentItemId, TvApp.getApplication().getCurrentUser().getId(), null, new Response<UserItemDataDto>() {
            @Override
            public void onResponse(UserItemDataDto response) {
                if (mCurrentActivity instanceof BaseActivity)
                    ((BaseActivity)mCurrentActivity).sendMessage(CustomMessage.RefreshCurrentItem);
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error setting played status");
                Utils.showToast(mCurrentActivity, R.string.playing_error);
            }
        });

    }

    private static void markUnplayed() {
        get(ApiClient.class).MarkUnplayedAsync(mCurrentItemId, TvApp.getApplication().getCurrentUser().getId(), new Response<UserItemDataDto>() {
            @Override
            public void onResponse(UserItemDataDto response) {
                if (mCurrentActivity instanceof BaseActivity)
                    ((BaseActivity)mCurrentActivity).sendMessage(CustomMessage.RefreshCurrentItem);
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error setting played status");
                Utils.showToast(mCurrentActivity, R.string.playing_error);
            }
        });

    }

    private static void toggleFavorite(boolean fav) {
        get(ApiClient.class).UpdateFavoriteStatusAsync(mCurrentItemId, TvApp.getApplication().getCurrentUser().getId(), fav, new Response<UserItemDataDto>() {
            @Override
            public void onResponse(UserItemDataDto response) {
                if (mCurrentActivity instanceof BaseActivity)
                    ((BaseActivity)mCurrentActivity).sendMessage(CustomMessage.RefreshCurrentItem);
                DataRefreshService dataRefreshService = get(DataRefreshService.class);
                dataRefreshService.setLastFavoriteUpdate(System.currentTimeMillis());
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error setting favorite status");
                Utils.showToast(mCurrentActivity, R.string.favorite_error);
            }
        });

    }

    private static void toggleLikes(Boolean likes) {
        if (likes == null) {
            get(ApiClient.class).ClearUserItemRatingAsync(mCurrentItemId, TvApp.getApplication().getCurrentUser().getId(), new Response<UserItemDataDto>() {
                @Override
                public void onResponse(UserItemDataDto response) {
                    if (mCurrentActivity instanceof BaseActivity)
                        ((BaseActivity)mCurrentActivity).sendMessage(CustomMessage.RefreshCurrentItem);
                }

                @Override
                public void onError(Exception exception) {
                    Timber.e(exception, "Error clearing like status");
                    Utils.showToast(mCurrentActivity, R.string.like_clearing_error);
                }
            });

        } else {
            get(ApiClient.class).UpdateUserItemRatingAsync(mCurrentItemId, TvApp.getApplication().getCurrentUser().getId(), likes, new Response<UserItemDataDto>() {
                @Override
                public void onResponse(UserItemDataDto response) {
                    if (mCurrentActivity instanceof BaseActivity)
                        ((BaseActivity)mCurrentActivity).sendMessage(CustomMessage.RefreshCurrentItem);
                }

                @Override
                public void onError(Exception exception) {
                    Timber.e(exception, "Error setting like status");
                    Utils.showToast(mCurrentActivity, R.string.like_setting_error);
                }
            });
        }

    }
}

