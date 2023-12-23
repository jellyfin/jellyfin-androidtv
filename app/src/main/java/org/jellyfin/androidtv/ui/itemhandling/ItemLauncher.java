package org.jellyfin.androidtv.ui.itemhandling;

import android.content.Context;

import androidx.annotation.Nullable;

import org.jellyfin.androidtv.constant.LiveTvOption;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.ChapterItemInfo;
import org.jellyfin.androidtv.preference.LibraryPreferences;
import org.jellyfin.androidtv.preference.PreferencesRepository;
import org.jellyfin.androidtv.ui.navigation.Destination;
import org.jellyfin.androidtv.ui.navigation.Destinations;
import org.jellyfin.androidtv.ui.navigation.NavigationRepository;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher;
import org.jellyfin.androidtv.ui.playback.VideoQueueManager;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper;
import org.jellyfin.androidtv.util.sdk.compat.FakeBaseItem;
import org.jellyfin.androidtv.util.sdk.compat.JavaCompat;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.PlayAccess;
import org.jellyfin.sdk.model.constant.CollectionType;
import org.jellyfin.sdk.model.serializer.UUIDSerializerKt;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ItemLauncher {
    public static void launchUserView(@Nullable final BaseItemDto baseItem) {
        Timber.d("**** Collection type: %s", baseItem.getCollectionType());

        NavigationRepository navigationRepository = KoinJavaComponent.<NavigationRepository>get(NavigationRepository.class);
        Destination destination = getUserViewDestination(baseItem);

        navigationRepository.navigate(destination);
    }

    public static Destination.Fragment getUserViewDestination(@Nullable final BaseItemDto baseItem) {
        String collectionType = baseItem == null ? null : baseItem.getCollectionType();
        if (collectionType == null) collectionType = "";

        switch (collectionType) {
            case CollectionType.Movies:
            case CollectionType.TvShows:
                LibraryPreferences displayPreferences = KoinJavaComponent.<PreferencesRepository>get(PreferencesRepository.class).getLibraryPreferences(baseItem.getDisplayPreferencesId());
                boolean enableSmartScreen = displayPreferences.get(LibraryPreferences.Companion.getEnableSmartScreen());

                if (!enableSmartScreen) return Destinations.INSTANCE.libraryBrowser(baseItem);
                else return Destinations.INSTANCE.librarySmartScreen(baseItem);
            case CollectionType.Music:
            case CollectionType.LiveTv:
                return Destinations.INSTANCE.librarySmartScreen(baseItem);
            default:
                return Destinations.INSTANCE.libraryBrowser(baseItem);
        }
    }

    public static void launch(final BaseRowItem rowItem, ItemRowAdapter adapter, int pos, final Context context) {
        NavigationRepository navigationRepository = KoinJavaComponent.<NavigationRepository>get(NavigationRepository.class);

        switch (rowItem.getBaseRowType()) {
            case BaseItem:
                BaseItemDto baseItem = rowItem.getBaseItem();
                try {
                    Timber.d("Item selected: %d - %s (%s)", rowItem.getIndex(), baseItem.getName(), baseItem.getType().toString());
                } catch (Exception e) {
                    //swallow it
                }

                MediaManager mediaManager = KoinJavaComponent.<MediaManager>get(MediaManager.class);
                //specialized type handling
                switch (baseItem.getType()) {
                    case USER_VIEW:
                    case COLLECTION_FOLDER:
                        launchUserView(baseItem);
                        return;
                    case SERIES:
                    case MUSIC_ARTIST:
                        navigationRepository.navigate(Destinations.INSTANCE.itemDetails(baseItem.getId()));
                        return;

                    case MUSIC_ALBUM:
                    case PLAYLIST:
                        navigationRepository.navigate(Destinations.INSTANCE.itemList(baseItem.getId()));
                        return;

                    case AUDIO:
                        Timber.d("got pos %s", pos);
                        if (rowItem.getBaseItem() == null)
                            return;

                        PlaybackLauncher playbackLauncher = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class);
                        if (playbackLauncher.interceptPlayRequest(context, rowItem.getBaseItem()))
                            return;

                        // if the song currently playing is selected (and is the exact item - this only happens in the nowPlayingRow), open AudioNowPlayingActivity
                        if (mediaManager.hasAudioQueueItems() && rowItem instanceof AudioQueueItem && rowItem.getBaseItem().getId().equals(mediaManager.getCurrentAudioItem().getId())) {
                            navigationRepository.navigate(Destinations.INSTANCE.getNowPlaying());
                        } else if (mediaManager.hasAudioQueueItems() && rowItem instanceof AudioQueueItem && pos < mediaManager.getCurrentAudioQueueSize()) {
                            Timber.d("playing audio queue item");
                            mediaManager.playFrom(pos);
                        } else if (adapter.getQueryType() == QueryType.Search) {
                            mediaManager.playNow(context, rowItem.getBaseItem());
                        } else {
                            Timber.d("playing audio item");
                            List<BaseItemDto> audioItemsAsList = new ArrayList<>();

                            for (Object item : adapter) {
                                if (item instanceof BaseRowItem && ((BaseRowItem) item).getBaseItem() != null)
                                    audioItemsAsList.add(((BaseRowItem) item).getBaseItem());
                            }
                            mediaManager.playNow(context, audioItemsAsList, pos, false);
                        }

                        return;
                    case SEASON:
                        navigationRepository.navigate(Destinations.INSTANCE.folderBrowser(baseItem));
                        return;

                    case BOX_SET:
                        navigationRepository.navigate(Destinations.INSTANCE.collectionBrowser(baseItem));
                        return;

                    case PHOTO:
                        navigationRepository.navigate(Destinations.INSTANCE.pictureViewer(
                                baseItem.getId(),
                                false,
                                adapter.getSortBy(),
                                adapter.getSortOrder()
                        ));
                        return;

                }

                // or generic handling
                if (baseItem.isFolder()) {
                    // Some items don't have a display preferences id, but it's required for StdGridFragment
                    // Use the id of the item as a workaround, it's a unique key for the specific item
                    // Which is exactly what we want
                    if (baseItem.getDisplayPreferencesId() == null) {
                        baseItem = JavaCompat.copyWithDisplayPreferencesId(baseItem, baseItem.getId().toString());
                    }

                    navigationRepository.navigate(Destinations.INSTANCE.libraryBrowser(baseItem));
                } else {
                    switch (rowItem.getSelectAction()) {

                        case ShowDetails:
                            navigationRepository.navigate(Destinations.INSTANCE.itemDetails(baseItem.getId()));
                            break;
                        case Play:
                            if (baseItem.getPlayAccess() == org.jellyfin.sdk.model.api.PlayAccess.FULL) {
                                //Just play it directly
                                final BaseItemKind itemType = baseItem.getType();
                                PlaybackHelper.getItemsToPlay(baseItem, baseItem.getType() == BaseItemKind.MOVIE, false, new Response<List<BaseItemDto>>() {
                                    @Override
                                    public void onResponse(List<BaseItemDto> response) {
                                        KoinJavaComponent.<VideoQueueManager>get(VideoQueueManager.class).setCurrentVideoQueue(response);
                                        Destination destination = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackDestination(itemType, 0);
                                        navigationRepository.navigate(destination);
                                    }
                                });
                            } else {
                                Utils.showToast(context, "Item not playable at this time");
                            }
                            break;
                    }
                }
                break;
            case Person:
                navigationRepository.navigate(Destinations.INSTANCE.itemDetails(rowItem.getBasePerson().getId()));

                break;
            case Chapter:
                final ChapterItemInfo chapter = rowItem.getChapterInfo();
                //Start playback of the item at the chapter point
                ItemLauncherHelper.getItem(chapter.getItemId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        List<BaseItemDto> items = new ArrayList<>();
                        items.add(response);
                        KoinJavaComponent.<VideoQueueManager>get(VideoQueueManager.class).setCurrentVideoQueue(items);
                        Long start = chapter.getStartPositionTicks() / 10000;
                        Destination destination = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackDestination(response.getType(), start.intValue());
                        navigationRepository.navigate(destination);
                    }
                });

                break;

            case LiveTvProgram:
                BaseItemDto program = rowItem.getBaseItem();
                switch (rowItem.getSelectAction()) {

                    case ShowDetails:
                        navigationRepository.navigate(Destinations.INSTANCE.channelDetails(program.getId(), program.getChannelId(), program));
                        break;
                    case Play:
                        if (program.getPlayAccess() == org.jellyfin.sdk.model.api.PlayAccess.FULL) {
                            //Just play it directly - need to retrieve program channel via items api to convert to BaseItem
                            ItemLauncherHelper.getItem(program.getChannelId(), new Response<BaseItemDto>() {
                                @Override
                                public void onResponse(BaseItemDto response) {
                                    List<BaseItemDto> items = new ArrayList<>();
                                    items.add(response);
                                    KoinJavaComponent.<VideoQueueManager>get(VideoQueueManager.class).setCurrentVideoQueue(items);
                                    Destination destination = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackDestination(response.getType(), 0);
                                    navigationRepository.navigate(destination);

                                }
                            });
                        } else {
                            Utils.showToast(context, "Item not playable at this time");
                        }
                }
                break;

            case LiveTvChannel:
                //Just tune to it by playing
                final BaseItemDto channel = rowItem.getBaseItem();
                ItemLauncherHelper.getItem(channel.getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        PlaybackHelper.getItemsToPlay(response, false, false, new Response<List<BaseItemDto>>() {
                            @Override
                            public void onResponse(List<BaseItemDto> response) {
                                KoinJavaComponent.<VideoQueueManager>get(VideoQueueManager.class).setCurrentVideoQueue(response);
                                Destination destination = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackDestination(channel.getType(), 0);
                                navigationRepository.navigate(destination);
                            }
                        });
                    }
                });
                break;

            case LiveTvRecording:
                switch (rowItem.getSelectAction()) {

                    case ShowDetails:
                        navigationRepository.navigate(Destinations.INSTANCE.itemDetails(rowItem.getBaseItem().getId()));
                        break;
                    case Play:
                        if (rowItem.getBaseItem().getPlayAccess() == PlayAccess.FULL) {
                            //Just play it directly but need to retrieve as base item
                            ItemLauncherHelper.getItem(rowItem.getBaseItem().getId(), new Response<BaseItemDto>() {
                                @Override
                                public void onResponse(BaseItemDto response) {
                                    List<BaseItemDto> items = new ArrayList<>();
                                    items.add(response);
                                    KoinJavaComponent.<VideoQueueManager>get(VideoQueueManager.class).setCurrentVideoQueue(items);
                                    Destination destination = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackDestination(rowItem.getBaseItemType(), 0);
                                    navigationRepository.navigate(destination);
                                }
                            });
                        } else {
                            Utils.showToast(context, "Item not playable at this time");
                        }
                        break;
                }
                break;

            case SeriesTimer:
                navigationRepository.navigate(Destinations.INSTANCE.seriesTimerDetails(UUIDSerializerKt.toUUID(rowItem.getItemId()), ModelCompat.asSdk(rowItem.getSeriesTimerInfo())));
                break;


            case GridButton:
                switch (rowItem.getGridButton().getId()) {
                    case LiveTvOption.LIVE_TV_GUIDE_OPTION_ID:
                        navigationRepository.navigate(Destinations.INSTANCE.getLiveTvGuide());
                        break;

                    case LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID:
                        navigationRepository.navigate(Destinations.INSTANCE.getLiveTvRecordings());
                        break;

                    case LiveTvOption.LIVE_TV_SERIES_OPTION_ID:
                        navigationRepository.navigate(Destinations.INSTANCE.librarySmartScreen(FakeBaseItem.INSTANCE.getSERIES_TIMERS()));
                        break;

                    case LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID:
                        navigationRepository.navigate(Destinations.INSTANCE.getLiveTvSchedule());
                        break;
                }
                break;
        }
    }
}
