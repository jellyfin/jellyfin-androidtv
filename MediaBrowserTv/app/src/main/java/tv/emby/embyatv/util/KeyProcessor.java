package tv.emby.embyatv.util;

import android.app.Activity;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.PopupMenu;

import java.util.Date;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemsResult;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.CustomMessage;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.querying.StdItemQuery;

/**
 * Created by Eric on 4/17/2015.
 */
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

    private static String mCurrentItemId;
    private static BaseActivity mCurrentActivity;
    private static boolean currentItemIsFolder = false;

    public static boolean HandleKey(int key, BaseRowItem rowItem, BaseActivity activity) {
        if (rowItem == null) return false;
        switch (key) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:

                switch (rowItem.getItemType()) {

                    case BaseItem:
                        BaseItemDto item = rowItem.getBaseItem();
                        switch (item.getType()) {
                            case "Movie":
                            case "Episode":
                            case "TvChannel":
                            case "Video":
                            case "Program":
                            case "ChannelVideoItem":
                                // give some audible feedback
                                Utils.Beep();
                                // retrieve full item and play
                                Utils.retrieveAndPlay(item.getId(), false, activity);
                                return true;
                            case "Series":
                            case "Season":
                            case "Folder":
                            case "BoxSet":
                                createPlayMenu(rowItem.getItemId(), true, activity);
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
                                // give some audible feedback
                                Utils.Beep();
                                // retrieve full item and play
                                Utils.retrieveAndPlay(rowItem.getItemId(), false, activity);
                                return true;
                            case "Series":
                            case "Season":
                            case "Folder":
                            case "BoxSet":
                                createPlayMenu(rowItem.getItemId(), true, activity);
                                return true;
                        }
                        break;
                    case LiveTvChannel:
                    case LiveTvRecording:
                        // give some audible feedback
                        Utils.Beep();
                        // retrieve full item and play
                        Utils.retrieveAndPlay(rowItem.getItemId(), false, activity);
                        return true;
                    case LiveTvProgram:
                        // give some audible feedback
                        Utils.Beep();
                        // retrieve channel this program belongs to and play
                        Utils.retrieveAndPlay(rowItem.getProgramInfo().getChannelId(), false, activity);
                        return true;
                    case GridButton:
                        break;
                }
                break;
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_BUTTON_Y:
                TvApp.getApplication().getLogger().Debug("Menu for: "+rowItem.getFullName());

                //Create a contextual menu based on item
                switch (rowItem.getItemType()) {

                    case BaseItem:
                        BaseItemDto item = rowItem.getBaseItem();
                        switch (item.getType()) {
                            case "Movie":
                            case "Episode":
                            case "TvChannel":
                            case "Video":
                            case "Program":
                            case "ChannelVideoItem":
                                // generate a standard item menu
                                createItemMenu(rowItem.getItemId(), item.getUserData(), false, activity);
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

    private static void createItemMenu(String itemId, UserItemDataDto userData, boolean isFolder, BaseActivity activity) {
        PopupMenu menu = Utils.createPopupMenu(activity, activity.getCurrentFocus(), Gravity.RIGHT);
        int order = 0;
        menu.getMenu().add(0, MENU_PLAY, order++, R.string.lbl_play);
        if (userData.getPlayed())
            menu.getMenu().add(0, MENU_UNMARK_PLAYED, order++, "Mark Un-played");
        else
            menu.getMenu().add(0, MENU_MARK_PLAYED, order++, "Mark Played");

        if (userData.getIsFavorite())
            menu.getMenu().add(0, MENU_UNMARK_FAVORITE, order++, "Remove Favorite");
        else
            menu.getMenu().add(0, MENU_MARK_FAVORITE, order++, "Add Favorite");

        if (userData.getLikes() == null) {
            menu.getMenu().add(0, MENU_LIKE, order++, "Like");
            menu.getMenu().add(0, MENU_DISLIKE, order++, "Dislike");
        } else if (userData.getLikes()) {
            menu.getMenu().add(0, MENU_UNLIKE, order++, "Unlike");
            menu.getMenu().add(0, MENU_DISLIKE, order++, "Dislike");
        } else {
            menu.getMenu().add(0, MENU_LIKE, order++, "Like");
            menu.getMenu().add(0, MENU_UNDISLIKE, order++, "Remove Dislike");
        }

        //Not sure I like this but I either duplicate processing with in-line events or do this and
        // use a single event handler
        mCurrentItemId = itemId;
        mCurrentActivity = activity;
        currentItemIsFolder = isFolder;

        menu.setOnMenuItemClickListener(menuItemClickListener);
        menu.show();

    }

    private static void createPlayMenu(String itemId, boolean isFolder, BaseActivity activity) {
        PopupMenu menu = Utils.createPopupMenu(activity, activity.getCurrentFocus(), Gravity.RIGHT);
        int order = 0;
        menu.getMenu().add(0, MENU_PLAY_FIRST_UNWATCHED, order++, R.string.lbl_play_first_unwatched);
        menu.getMenu().add(0, MENU_PLAY, order++, R.string.lbl_play_all);
        menu.getMenu().add(0, MENU_PLAY_SHUFFLE, order++, R.string.lbl_shuffle_all);

        //Not sure I like this but I either duplicate processing with in-line events or do this and
        // use a single event handler
        mCurrentItemId = itemId;
        mCurrentActivity = activity;
        currentItemIsFolder = isFolder;

        menu.setOnMenuItemClickListener(menuItemClickListener);
        menu.show();

    }

    private static PopupMenu.OnMenuItemClickListener menuItemClickListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Utils.Beep();

            switch (item.getItemId()) {
                case MENU_PLAY:
                    Utils.retrieveAndPlay(mCurrentItemId, false, mCurrentActivity);
                    return true;
                case MENU_PLAY_SHUFFLE:
                    Utils.retrieveAndPlay(mCurrentItemId, true, mCurrentActivity);
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
                    TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
                        @Override
                        public void onResponse(ItemsResult response) {
                            if (response.getTotalRecordCount() == 0) {
                                Utils.showToast(mCurrentActivity, "No items to play");
                            } else {
                                Utils.retrieveAndPlay(response.getItems()[0].getId(), false, mCurrentActivity);
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            TvApp.getApplication().getLogger().ErrorException("Error trying to play first unwatched", exception);
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
                    TvApp.getApplication().getApiClient().MarkPlayedAsync(mCurrentItemId, TvApp.getApplication().getCurrentUser().getId(), null, new Response<UserItemDataDto>() {
                        @Override
                        public void onResponse(UserItemDataDto response) {
                            mCurrentActivity.sendMessage(CustomMessage.RefreshCurrentItem);
                        }

                        @Override
                        public void onError(Exception exception) {
                            TvApp.getApplication().getLogger().ErrorException("Error setting played status", exception);
                            Utils.showToast(mCurrentActivity, "Error setting played status");
                        }
                    });
                    return true;
                case MENU_UNMARK_PLAYED:
                    TvApp.getApplication().getApiClient().MarkUnplayedAsync(mCurrentItemId, TvApp.getApplication().getCurrentUser().getId(), new Response<UserItemDataDto>() {
                        @Override
                        public void onResponse(UserItemDataDto response) {
                            mCurrentActivity.sendMessage(CustomMessage.RefreshCurrentItem);
                        }

                        @Override
                        public void onError(Exception exception) {
                            TvApp.getApplication().getLogger().ErrorException("Error setting played status", exception);
                            Utils.showToast(mCurrentActivity, "Error setting played status");
                        }
                    });
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
            }

            return false;
            }
    };

    private static void toggleFavorite(boolean fav) {
        TvApp.getApplication().getApiClient().UpdateFavoriteStatusAsync(mCurrentItemId, TvApp.getApplication().getCurrentUser().getId(), fav, new Response<UserItemDataDto>() {
            @Override
            public void onResponse(UserItemDataDto response) {
                mCurrentActivity.sendMessage(CustomMessage.RefreshCurrentItem);
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error setting favorite status", exception);
                Utils.showToast(mCurrentActivity, "Error setting favorite status");
            }
        });

    }

    private static void toggleLikes(Boolean likes) {
        if (likes == null) {
            TvApp.getApplication().getApiClient().ClearUserItemRatingAsync(mCurrentItemId, TvApp.getApplication().getCurrentUser().getId(), new Response<UserItemDataDto>() {
                @Override
                public void onResponse(UserItemDataDto response) {
                    mCurrentActivity.sendMessage(CustomMessage.RefreshCurrentItem);
                }

                @Override
                public void onError(Exception exception) {
                    TvApp.getApplication().getLogger().ErrorException("Error clearing like status", exception);
                    Utils.showToast(mCurrentActivity, "Error clearing like status");
                }
            });

        } else {
            TvApp.getApplication().getApiClient().UpdateUserItemRatingAsync(mCurrentItemId, TvApp.getApplication().getCurrentUser().getId(), likes, new Response<UserItemDataDto>() {
                @Override
                public void onResponse(UserItemDataDto response) {
                    mCurrentActivity.sendMessage(CustomMessage.RefreshCurrentItem);
                }

                @Override
                public void onError(Exception exception) {
                    TvApp.getApplication().getLogger().ErrorException("Error setting like status", exception);
                    Utils.showToast(mCurrentActivity, "Error setting like status");
                }
            });
        }

    }
}

