package org.jellyfin.androidtv.util;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.PopupMenu;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.data.repository.CustomMessageRepository;
import org.jellyfin.androidtv.data.repository.ItemMutationRepository;
import org.jellyfin.androidtv.ui.itemhandling.AudioQueueItem;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowType;
import org.jellyfin.androidtv.ui.navigation.Destinations;
import org.jellyfin.androidtv.ui.navigation.NavigationRepository;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper;
import org.jellyfin.androidtv.util.sdk.BaseItemExtensionsKt;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.entities.SortOrder;
import org.jellyfin.apiclient.model.querying.ItemFilter;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.ItemSortBy;
import org.jellyfin.sdk.model.api.MediaType;
import org.jellyfin.sdk.model.api.UserItemDataDto;
import org.jellyfin.sdk.model.serializer.UUIDSerializerKt;
import org.koin.java.KoinJavaComponent;

import java.util.List;
import java.util.UUID;

import kotlin.Lazy;
import timber.log.Timber;

public class KeyProcessor {
    public static final int MENU_MARK_FAVORITE = 0;
    public static final int MENU_UNMARK_FAVORITE = 1;
    public static final int MENU_MARK_PLAYED = 2;
    public static final int MENU_UNMARK_PLAYED = 3;
    public static final int MENU_PLAY = 4;
    public static final int MENU_PLAY_SHUFFLE = 5;
    public static final int MENU_PLAY_FIRST_UNWATCHED = 6;
    public static final int MENU_ADD_QUEUE = 7;
    public static final int MENU_ADVANCE_QUEUE = 8;
    public static final int MENU_REMOVE_FROM_QUEUE = 9;
    public static final int MENU_GOTO_NOW_PLAYING = 10;
    public static final int MENU_INSTANT_MIX = 11;
    public static final int MENU_CLEAR_QUEUE = 12;
    public static final int MENU_TOGGLE_SHUFFLE = 13;

    private final Lazy<MediaManager> mediaManager = KoinJavaComponent.<MediaManager>inject(MediaManager.class);
    private final Lazy<NavigationRepository> navigationRepository = KoinJavaComponent.<NavigationRepository>inject(NavigationRepository.class);
    private final Lazy<ItemMutationRepository> itemMutationRepository = KoinJavaComponent.<ItemMutationRepository>inject(ItemMutationRepository.class);
    private final Lazy<CustomMessageRepository> customMessageRepository = KoinJavaComponent.<CustomMessageRepository>inject(CustomMessageRepository.class);
    private final Lazy<ApiClient> apiClient = KoinJavaComponent.<ApiClient>inject(org.jellyfin.apiclient.interaction.ApiClient.class);

    public boolean handleKey(int key, BaseRowItem rowItem, FragmentActivity activity) {
        if (rowItem == null) return false;

        switch (key) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (mediaManager.getValue().isPlayingAudio() && (rowItem.getBaseRowType() != BaseRowType.BaseItem || rowItem.getBaseItemType() != BaseItemKind.PHOTO)) {
                    // Rewrite uses media sessions which the system automatically manipulates on key presses
                    return false;
                }

                switch (rowItem.getBaseRowType()) {

                    case BaseItem:
                        BaseItemDto item = rowItem.getBaseItem();
                        if (!BaseItemExtensionsKt.canPlay(item)) return false;
                        switch (item.getType()) {
                            case AUDIO:
                                if (rowItem instanceof AudioQueueItem) {
                                    createItemMenu(rowItem, item.getUserData(), activity);
                                    return true;
                                }
                                //fall through...
                            case MOVIE:
                            case EPISODE:
                            case TV_CHANNEL:
                            case VIDEO:
                            case PROGRAM:
                            case TRAILER:
                                // retrieve full item and play
                                PlaybackHelper.retrieveAndPlay(item.getId(), false, activity);
                                return true;
                            case SERIES:
                            case SEASON:
                            case BOX_SET:
                                createPlayMenu(rowItem.getBaseItem(), false, activity);
                                return true;
                            case MUSIC_ALBUM:
                            case MUSIC_ARTIST:
                                createPlayMenu(rowItem.getBaseItem(), true, activity);
                                return true;
                            case PLAYLIST:
                                createPlayMenu(rowItem.getBaseItem(), MediaType.AUDIO.equals(item.getMediaType()), activity);
                                return true;
                            case PHOTO:
                                navigationRepository.getValue().navigate(Destinations.INSTANCE.pictureViewer(
                                        rowItem.getBaseItem().getId(),
                                        true,
                                        ItemSortBy.SORT_NAME,
                                        org.jellyfin.sdk.model.api.SortOrder.ASCENDING
                                ));
                                return true;
                        }
                        break;
                    case Person:
                        break;
                    case Chapter:
                        break;
                    case LiveTvChannel:
                    case LiveTvRecording:
                        // retrieve full item and play
                        PlaybackHelper.retrieveAndPlay(UUIDSerializerKt.toUUID(rowItem.getItemId()), false, activity);
                        return true;
                    case LiveTvProgram:
                        // retrieve channel this program belongs to and play
                        PlaybackHelper.retrieveAndPlay(rowItem.getBaseItem().getChannelId(), false, activity);
                        return true;
                    case GridButton:
                        break;
                }

                // Rewrite uses media sessions which the system automatically manipulates on key presses
                if (mediaManager.getValue().hasAudioQueueItems()) return false;

                break;
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_BUTTON_Y:
                Timber.d("Menu for: %s", rowItem.getFullName(activity));

                //Create a contextual menu based on item
                switch (rowItem.getBaseRowType()) {
                    case BaseItem:
                        BaseItemDto item = rowItem.getBaseItem();
                        switch (item.getType()) {
                            case MOVIE:
                            case EPISODE:
                            case TV_CHANNEL:
                            case VIDEO:
                            case PROGRAM:
                            case SERIES:
                            case SEASON:
                            case BOX_SET:
                            case MUSIC_ALBUM:
                            case MUSIC_ARTIST:
                            case PLAYLIST:
                            case AUDIO:
                            case TRAILER:
                                // generate a standard item menu
                                createItemMenu(rowItem, item.getUserData(), activity);
                                break;
                        }
                        break;
                    case Person:
                        break;
                    case Chapter:
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

    public PopupMenu createItemMenu(BaseRowItem rowItem, UserItemDataDto userData, FragmentActivity activity) {
        BaseItemDto item = rowItem.getBaseItem();
        PopupMenu menu = new PopupMenu(activity, activity.getCurrentFocus(), Gravity.END);
        int order = 0;

        if (rowItem instanceof AudioQueueItem) {
            if (rowItem.getBaseItem() != mediaManager.getValue().getCurrentAudioItem())
                menu.getMenu().add(0, MENU_ADVANCE_QUEUE, order++, R.string.lbl_play_from_here);
            menu.getMenu().add(0, MENU_GOTO_NOW_PLAYING, order++, R.string.lbl_goto_now_playing);

            if (mediaManager.getValue().getCurrentAudioQueue().size() > 1) {
                menu.getMenu().add(0, MENU_TOGGLE_SHUFFLE, order++, R.string.lbl_shuffle_queue);
            }

            if (userData != null) {
                if (userData.isFavorite()) {
                    menu.getMenu().add(0, MENU_UNMARK_FAVORITE, order++, activity.getString(R.string.lbl_remove_favorite));
                } else {
                    menu.getMenu().add(0, MENU_MARK_FAVORITE, order++, activity.getString(R.string.lbl_add_favorite));
                }
            }

            // don't allow removal of last item - framework will crash trying to animate an empty row
            if (mediaManager.getValue().getCurrentAudioQueue().size() > 1) {
                menu.getMenu().add(0, MENU_REMOVE_FROM_QUEUE, order++, R.string.lbl_remove_from_queue);
            }

            if (mediaManager.getValue().hasAudioQueueItems()) {
                menu.getMenu().add(0, MENU_CLEAR_QUEUE, order++, R.string.lbl_clear_queue);
            }
        } else {
            boolean isFolder = item.isFolder() != null && item.isFolder();
            if (BaseItemExtensionsKt.canPlay(item)) {
                if (isFolder
                        && item.getType() != BaseItemKind.MUSIC_ALBUM
                        && item.getType() != BaseItemKind.PLAYLIST
                        && item.getType() != BaseItemKind.MUSIC_ARTIST
                        && userData != null
                        && userData.getUnplayedItemCount() != null
                        && userData.getUnplayedItemCount() > 0) {
                    menu.getMenu().add(0, MENU_PLAY_FIRST_UNWATCHED, order++, R.string.lbl_play_first_unwatched);
                }
                menu.getMenu().add(0, MENU_PLAY, order++, isFolder ? R.string.lbl_play_all : R.string.lbl_play);
                if (isFolder) {
                    menu.getMenu().add(0, MENU_PLAY_SHUFFLE, order++, R.string.lbl_shuffle_all);
                }
            }

            boolean isMusic = item.getType() == BaseItemKind.MUSIC_ALBUM
                    || item.getType() == BaseItemKind.MUSIC_ARTIST
                    || item.getType() == BaseItemKind.AUDIO
                    || (item.getType() == BaseItemKind.PLAYLIST && MediaType.AUDIO.equals(item.getMediaType()));

            if (isMusic) {
                menu.getMenu().add(0, MENU_ADD_QUEUE, order++, R.string.lbl_add_to_queue);
            }

            if (isMusic) {
                if (item.getType() != BaseItemKind.PLAYLIST) {
                    menu.getMenu().add(0, MENU_INSTANT_MIX, order++, R.string.lbl_instant_mix);
                }
            } else {
                if (userData != null && userData.getPlayed()) {
                    menu.getMenu().add(0, MENU_UNMARK_PLAYED, order++, activity.getString(R.string.lbl_mark_unplayed));
                } else {
                    menu.getMenu().add(0, MENU_MARK_PLAYED, order++, activity.getString(R.string.lbl_mark_played));
                }
            }

            if (userData != null) {
                if (userData.isFavorite()) {
                    menu.getMenu().add(0, MENU_UNMARK_FAVORITE, order++, activity.getString(R.string.lbl_remove_favorite));
                } else {
                    menu.getMenu().add(0, MENU_MARK_FAVORITE, order++, activity.getString(R.string.lbl_add_favorite));
                }
            }
        }

        menu.setOnMenuItemClickListener(new KeyProcessorItemMenuClickListener(activity, rowItem.getBaseItem(), rowItem.getIndex()));
        menu.show();
        return menu;
    }

    private void createPlayMenu(BaseItemDto item, boolean isMusic, FragmentActivity activity) {
        PopupMenu menu = new PopupMenu(activity, activity.getCurrentFocus(), Gravity.END);
        int order = 0;
        if (!isMusic && item.getType() != BaseItemKind.PLAYLIST) {
            menu.getMenu().add(0, MENU_PLAY_FIRST_UNWATCHED, order++, R.string.lbl_play_first_unwatched);
        }
        menu.getMenu().add(0, MENU_PLAY, order++, R.string.lbl_play_all);
        menu.getMenu().add(0, MENU_PLAY_SHUFFLE, order++, R.string.lbl_shuffle_all);
        if (isMusic) {
            menu.getMenu().add(0, MENU_ADD_QUEUE, order, R.string.lbl_add_to_queue);
        }

        menu.setOnMenuItemClickListener(new KeyProcessorItemMenuClickListener(activity, item, -1));
        menu.show();
    }

    private class KeyProcessorItemMenuClickListener implements PopupMenu.OnMenuItemClickListener {
        private BaseItemDto item;
        private FragmentActivity activity;
        private int rowIndex;

        private KeyProcessorItemMenuClickListener(FragmentActivity activity, BaseItemDto item, int rowIndex) {
            this.item = item;
            this.activity = activity;
            this.rowIndex = rowIndex;
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case MENU_PLAY:
                    PlaybackHelper.retrieveAndPlay(item.getId(), false, activity);
                    return true;
                case MENU_PLAY_SHUFFLE:
                    PlaybackHelper.retrieveAndPlay(item.getId(), true, activity);
                    return true;
                case MENU_ADD_QUEUE:
                    PlaybackHelper.getItemsToPlay(item, false, false, new Response<List<BaseItemDto>>() {
                        @Override
                        public void onResponse(List<BaseItemDto> response) {
                            mediaManager.getValue().addToAudioQueue(response);
                        }

                        @Override
                        public void onError(Exception exception) {
                            Utils.showToast(activity, R.string.msg_cannot_play_time);
                        }
                    });
                    return true;
                case MENU_PLAY_FIRST_UNWATCHED:
                    StdItemQuery query = new StdItemQuery();
                    query.setParentId(item.getId().toString());
                    query.setRecursive(true);
                    query.setIsVirtualUnaired(false);
                    query.setIsMissing(false);
                    query.setSortBy(new String[]{ItemSortBy.SORT_NAME.getSerialName()});
                    query.setSortOrder(SortOrder.Ascending);
                    query.setLimit(1);
                    query.setExcludeItemTypes(new String[]{"Series", "Season", "Folder", "MusicAlbum", "Playlist", "BoxSet"});
                    query.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
                    apiClient.getValue().GetItemsAsync(query, new Response<ItemsResult>() {
                        @Override
                        public void onResponse(ItemsResult response) {
                            if (response.getTotalRecordCount() == 0) {
                                Utils.showToast(activity, R.string.msg_no_items);
                            } else {
                                PlaybackHelper.retrieveAndPlay(UUIDSerializerKt.toUUID(response.getItems()[0].getId()), false, activity);
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            Timber.e(exception, "Error trying to play first unwatched");
                            Utils.showToast(activity, R.string.msg_video_playback_error);
                        }
                    });
                    return true;
                case MENU_MARK_FAVORITE:
                    toggleFavorite(activity, item.getId(), true);
                    return true;
                case MENU_UNMARK_FAVORITE:
                    toggleFavorite(activity, item.getId(), false);
                    return true;
                case MENU_MARK_PLAYED:
                    togglePlayed(activity, item.getId(), true);
                    return true;
                case MENU_UNMARK_PLAYED:
                    togglePlayed(activity, item.getId(), false);
                    return true;
                case MENU_GOTO_NOW_PLAYING:
                    navigationRepository.getValue().navigate(Destinations.INSTANCE.getNowPlaying());
                    return true;
                case MENU_TOGGLE_SHUFFLE:
                    mediaManager.getValue().shuffleAudioQueue();
                    return true;
                case MENU_REMOVE_FROM_QUEUE:
                    mediaManager.getValue().removeFromAudioQueue(rowIndex);
                    return true;
                case MENU_ADVANCE_QUEUE:
                    mediaManager.getValue().playFrom(rowIndex);
                    return true;
                case MENU_CLEAR_QUEUE:
                    mediaManager.getValue().clearAudioQueue();
                    return true;
                case MENU_INSTANT_MIX:
                    PlaybackHelper.playInstantMix(activity, item);
                    return true;
            }

            return false;
        }
    };

    private void togglePlayed(LifecycleOwner lifecycleOwner, UUID itemId, boolean played) {

        CoroutineUtils.runOnLifecycle(lifecycleOwner.getLifecycle(), (scope, continuation) ->
                itemMutationRepository.getValue().setPlayed(itemId, played, continuation)
        );

        customMessageRepository.getValue().pushMessage(CustomMessage.RefreshCurrentItem.INSTANCE);
    }

    private void toggleFavorite(LifecycleOwner lifecycleOwner, UUID itemId, boolean favorite) {
        CoroutineUtils.runOnLifecycle(lifecycleOwner.getLifecycle(), (scope, continuation) ->
                itemMutationRepository.getValue().setFavorite(itemId, favorite, continuation)
        );

        customMessageRepository.getValue().pushMessage(CustomMessage.RefreshCurrentItem.INSTANCE);
    }
}

