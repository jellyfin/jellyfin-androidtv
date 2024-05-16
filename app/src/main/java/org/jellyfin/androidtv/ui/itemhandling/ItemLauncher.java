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
import org.jellyfin.androidtv.util.sdk.compat.JavaCompat;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.CollectionType;
import org.jellyfin.sdk.model.serializer.UUIDSerializerKt;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

public class ItemLauncher {
    private final Lazy<NavigationRepository> navigationRepository = KoinJavaComponent.<NavigationRepository>inject(NavigationRepository.class);
    private final Lazy<PreferencesRepository> preferencesRepository = KoinJavaComponent.<PreferencesRepository>inject(org.jellyfin.androidtv.preference.PreferencesRepository .class);
    private final Lazy<MediaManager> mediaManager = KoinJavaComponent.<MediaManager>inject(MediaManager.class);
    private final Lazy<VideoQueueManager> videoQueueManager = KoinJavaComponent.<VideoQueueManager>inject(VideoQueueManager.class);
    private final Lazy<PlaybackLauncher> playbackLauncher = KoinJavaComponent.<PlaybackLauncher>inject(PlaybackLauncher.class);

    public void launchUserView(@Nullable final BaseItemDto baseItem) {
        Timber.d("**** Collection type: %s", baseItem.getCollectionType());

        Destination destination = getUserViewDestination(baseItem);

        navigationRepository.getValue().navigate(destination);
    }

    public Destination.Fragment getUserViewDestination(@Nullable final BaseItemDto baseItem) {
        CollectionType collectionType = baseItem == null ? CollectionType.UNKNOWN : baseItem.getCollectionType();

        switch (collectionType) {
            case MOVIES:
            case TVSHOWS:
                LibraryPreferences displayPreferences = preferencesRepository.getValue().getLibraryPreferences(baseItem.getDisplayPreferencesId());
                boolean enableSmartScreen = displayPreferences.get(LibraryPreferences.Companion.getEnableSmartScreen());

                if (!enableSmartScreen) return Destinations.INSTANCE.libraryBrowser(baseItem);
                else return Destinations.INSTANCE.librarySmartScreen(baseItem);
            case MUSIC:
            case LIVETV:
                return Destinations.INSTANCE.librarySmartScreen(baseItem);
            default:
                return Destinations.INSTANCE.libraryBrowser(baseItem);
        }
    }

    public void launch(final BaseRowItem rowItem, ItemRowAdapter adapter, int pos, final Context context) {
        switch (rowItem.getBaseRowType()) {
            case BaseItem:
                BaseItemDto baseItem = rowItem.getBaseItem();
                try {
                    Timber.d("Item selected: %d - %s (%s)", rowItem.getIndex(), baseItem.getName(), baseItem.getType().toString());
                } catch (Exception e) {
                    //swallow it
                }

                //specialized type handling
                switch (baseItem.getType()) {
                    case USER_VIEW:
                    case COLLECTION_FOLDER:
                        launchUserView(baseItem);
                        return;
                    case SERIES:
                    case MUSIC_ARTIST:
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.itemDetails(baseItem.getId()));
                        return;

                    case MUSIC_ALBUM:
                    case PLAYLIST:
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.itemList(baseItem.getId()));
                        return;

                    case AUDIO:
                        Timber.d("got pos %s", pos);
                        if (rowItem.getBaseItem() == null)
                            return;

                        // if the song currently playing is selected (and is the exact item - this only happens in the nowPlayingRow), open AudioNowPlayingActivity
                        if (mediaManager.getValue().hasAudioQueueItems() && rowItem instanceof AudioQueueItem && rowItem.getBaseItem().getId().equals(mediaManager.getValue().getCurrentAudioItem().getId())) {
                            navigationRepository.getValue().navigate(Destinations.INSTANCE.getNowPlaying());
                        } else if (mediaManager.getValue().hasAudioQueueItems() && rowItem instanceof AudioQueueItem && pos < mediaManager.getValue().getCurrentAudioQueueSize()) {
                            Timber.d("playing audio queue item");
                            mediaManager.getValue().playFrom(pos);
                        } else if (adapter.getQueryType() == QueryType.Search) {
                            mediaManager.getValue().playNow(context, Arrays.asList(rowItem.getBaseItem()), 0, false);
                        } else {
                            Timber.d("playing audio item");
                            List<BaseItemDto> audioItemsAsList = new ArrayList<>();

                            for (Object item : adapter) {
                                if (item instanceof BaseRowItem && ((BaseRowItem) item).getBaseItem() != null)
                                    audioItemsAsList.add(((BaseRowItem) item).getBaseItem());
                            }
                            mediaManager.getValue().playNow(context, audioItemsAsList, pos, false);
                        }

                        return;
                    case SEASON:
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.folderBrowser(baseItem));
                        return;

                    case BOX_SET:
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.collectionBrowser(baseItem));
                        return;

                    case PHOTO:
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.pictureViewer(
                                baseItem.getId(),
                                false,
                                adapter.getSortBy(),
                                adapter.getSortOrder()
                        ));
                        return;

                }

                // or generic handling
                if (Utils.isTrue(baseItem.isFolder())) {
                    // Some items don't have a display preferences id, but it's required for StdGridFragment
                    // Use the id of the item as a workaround, it's a unique key for the specific item
                    // Which is exactly what we want
                    if (baseItem.getDisplayPreferencesId() == null) {
                        baseItem = JavaCompat.copyWithDisplayPreferencesId(baseItem, baseItem.getId().toString());
                    }

                    navigationRepository.getValue().navigate(Destinations.INSTANCE.libraryBrowser(baseItem));
                } else {
                    switch (rowItem.getSelectAction()) {

                        case ShowDetails:
                            navigationRepository.getValue().navigate(Destinations.INSTANCE.itemDetails(baseItem.getId()));
                            break;
                        case Play:
                            //Just play it directly
                            final BaseItemKind itemType = baseItem.getType();
                            PlaybackHelper.getItemsToPlay(baseItem, baseItem.getType() == BaseItemKind.MOVIE, false, new Response<List<BaseItemDto>>() {
                                @Override
                                public void onResponse(List<BaseItemDto> response) {
                                    videoQueueManager.getValue().setCurrentVideoQueue(response);
                                    Destination destination = playbackLauncher.getValue().getPlaybackDestination(itemType, 0);
                                    navigationRepository.getValue().navigate(destination);
                                }
                            });
                            break;
                    }
                }
                break;
            case Person:
                navigationRepository.getValue().navigate(Destinations.INSTANCE.itemDetails(rowItem.getBasePerson().getId()));

                break;
            case Chapter:
                final ChapterItemInfo chapter = rowItem.getChapterInfo();
                //Start playback of the item at the chapter point
                ItemLauncherHelper.getItem(chapter.getItemId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        List<BaseItemDto> items = new ArrayList<>();
                        items.add(response);
                        videoQueueManager.getValue().setCurrentVideoQueue(items);
                        Long start = chapter.getStartPositionTicks() / 10000;
                        Destination destination = playbackLauncher.getValue().getPlaybackDestination(response.getType(), start.intValue());
                        navigationRepository.getValue().navigate(destination);
                    }
                });

                break;

            case LiveTvProgram:
                BaseItemDto program = rowItem.getBaseItem();
                switch (rowItem.getSelectAction()) {

                    case ShowDetails:
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.channelDetails(program.getId(), program.getChannelId(), program));
                        break;
                    case Play:
                        //Just play it directly - need to retrieve program channel via items api to convert to BaseItem
                        ItemLauncherHelper.getItem(program.getChannelId(), new Response<BaseItemDto>() {
                            @Override
                            public void onResponse(BaseItemDto response) {
                                List<BaseItemDto> items = new ArrayList<>();
                                items.add(response);
                                videoQueueManager.getValue().setCurrentVideoQueue(items);
                                Destination destination = playbackLauncher.getValue().getPlaybackDestination(response.getType(), 0);
                                navigationRepository.getValue().navigate(destination);

                            }
                        });
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
                                videoQueueManager.getValue().setCurrentVideoQueue(response);
                                Destination destination = playbackLauncher.getValue().getPlaybackDestination(channel.getType(), 0);
                                navigationRepository.getValue().navigate(destination);
                            }
                        });
                    }
                });
                break;

            case LiveTvRecording:
                switch (rowItem.getSelectAction()) {

                    case ShowDetails:
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.itemDetails(rowItem.getBaseItem().getId()));
                        break;
                    case Play:
                        //Just play it directly but need to retrieve as base item
                        ItemLauncherHelper.getItem(rowItem.getBaseItem().getId(), new Response<BaseItemDto>() {
                            @Override
                            public void onResponse(BaseItemDto response) {
                                List<BaseItemDto> items = new ArrayList<>();
                                items.add(response);
                                videoQueueManager.getValue().setCurrentVideoQueue(items);
                                Destination destination = playbackLauncher.getValue().getPlaybackDestination(rowItem.getBaseItemType(), 0);
                                navigationRepository.getValue().navigate(destination);
                            }
                        });
                        break;
                }
                break;

            case SeriesTimer:
                navigationRepository.getValue().navigate(Destinations.INSTANCE.seriesTimerDetails(UUIDSerializerKt.toUUID(rowItem.getItemId()), ModelCompat.asSdk(rowItem.getSeriesTimerInfo())));
                break;


            case GridButton:
                switch (rowItem.getGridButton().getId()) {
                    case LiveTvOption.LIVE_TV_GUIDE_OPTION_ID:
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.getLiveTvGuide());
                        break;

                    case LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID:
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.getLiveTvRecordings());
                        break;

                    case LiveTvOption.LIVE_TV_SERIES_OPTION_ID:
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.getLiveTvSeriesRecordings());
                        break;

                    case LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID:
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.getLiveTvSchedule());
                        break;
                }
                break;
        }
    }
}
