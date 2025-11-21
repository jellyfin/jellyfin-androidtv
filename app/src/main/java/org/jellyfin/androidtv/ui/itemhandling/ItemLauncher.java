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
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter;
import org.jellyfin.androidtv.util.PlaybackHelper;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.Response;
import org.jellyfin.androidtv.util.sdk.compat.JavaCompat;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.CollectionType;
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
    private final Lazy<PlaybackLauncher> playbackLauncher = KoinJavaComponent.<PlaybackLauncher>inject(PlaybackLauncher.class);
    private final Lazy<PlaybackHelper> playbackHelper = KoinJavaComponent.<PlaybackHelper>inject(PlaybackHelper.class);

    public void launchUserView(@Nullable final BaseItemDto baseItem) {
        Timber.d("**** Collection type: %s", baseItem.getCollectionType());

        Destination destination = getUserViewDestination(baseItem);

        navigationRepository.getValue().navigate(destination);
    }

    public Destination.Fragment getUserViewDestination(@Nullable final BaseItemDto baseItem) {
        CollectionType collectionType = baseItem == null ? CollectionType.UNKNOWN : baseItem.getCollectionType();
        if (collectionType == null) collectionType = CollectionType.UNKNOWN;

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

    public void launch(final BaseRowItem rowItem, MutableObjectAdapter<Object> adapter, final Context context) {
        switch (rowItem.getBaseRowType()) {
            case BaseItem:
                BaseItemDto baseItem = rowItem.getBaseItem();
                try {
                    Timber.i("Item selected: %s (%s)", baseItem.getName(), baseItem.getType().toString());
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
                        if (rowItem.getBaseItem() == null)
                            return;

                        // if the song currently playing is selected (and is the exact item - this only happens in the nowPlayingRow), open AudioNowPlayingActivity
                        if (mediaManager.getValue().hasAudioQueueItems() && rowItem instanceof AudioQueueBaseRowItem && rowItem.getBaseItem().getId().equals(mediaManager.getValue().getCurrentAudioItem().getId())) {
                            navigationRepository.getValue().navigate(Destinations.INSTANCE.getNowPlaying());
                        } else if (mediaManager.getValue().hasAudioQueueItems() && rowItem instanceof AudioQueueBaseRowItem && adapter.indexOf(rowItem) < mediaManager.getValue().getCurrentAudioQueueSize()) {
                            Timber.d("playing audio queue item");
                            mediaManager.getValue().playFrom(((AudioQueueBaseRowItem) rowItem).getQueueEntry());
                        } else if (adapter instanceof ItemRowAdapter && ((ItemRowAdapter)adapter).getQueryType() == QueryType.Search) {
                            playbackLauncher.getValue().launch(context, Arrays.asList(rowItem.getBaseItem()));
                        } else {
                            Timber.d("playing audio item");
                            List<BaseItemDto> audioItemsAsList = new ArrayList<>();

                            for (Object item : adapter) {
                                if (item instanceof BaseRowItem && ((BaseRowItem) item).getBaseItem() != null)
                                    audioItemsAsList.add(((BaseRowItem) item).getBaseItem());
                            }

                            playbackLauncher.getValue().launch(context, audioItemsAsList, 0, false, adapter.indexOf(rowItem));
                        }

                        return;
                    case SEASON:
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.folderBrowser(baseItem));
                        return;

                    case BOX_SET:
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.collectionBrowser(baseItem));
                        return;

                    case PHOTO:
                        if (adapter instanceof ItemRowAdapter) {
                            navigationRepository.getValue().navigate(Destinations.INSTANCE.photoPlayer(
                                    baseItem.getId(),
                                    false,
                                    ((ItemRowAdapter) adapter).getSortBy(),
                                    ((ItemRowAdapter) adapter).getSortOrder()
                            ));
                        }
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
                            playbackHelper.getValue().getItemsToPlay(context, baseItem, baseItem.getType() == BaseItemKind.MOVIE, false, new Response<List<BaseItemDto>>() {
                                @Override
                                public void onResponse(List<BaseItemDto> response) {
                                    if (!isActive()) return;
                                    playbackLauncher.getValue().launch(context, response);
                                }
                            });
                            break;
                    }
                }
                break;
            case Person:
                navigationRepository.getValue().navigate(Destinations.INSTANCE.itemDetails(rowItem.getItemId()));

                break;
            case Chapter:
                final ChapterItemInfo chapter = ((ChapterItemInfoBaseRowItem) rowItem).getChapterInfo();
                //Start playback of the item at the chapter point
                ItemLauncherHelper.getItem(rowItem.getItemId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        if (!isActive()) return;
                        List<BaseItemDto> items = new ArrayList<>(1);
                        items.add(response);
                        Long start = chapter.getStartPositionTicks() / 10000;
                        playbackLauncher.getValue().launch(context, items, start.intValue());
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
                                if (!isActive()) return;
                                List<BaseItemDto> items = new ArrayList<>(1);
                                items.add(response);
                                playbackLauncher.getValue().launch(context, items);

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
                        if (!isActive()) return;
                        playbackHelper.getValue().getItemsToPlay(context, response, false, false, new Response<List<BaseItemDto>>() {
                            @Override
                            public void onResponse(List<BaseItemDto> response) {
                                if (!isActive()) return;
                                playbackLauncher.getValue().launch(context, response);
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
                                if (!isActive()) return;
                                List<BaseItemDto> items = new ArrayList<>(1);
                                items.add(response);
                                playbackLauncher.getValue().launch(context, items);
                            }
                        });
                        break;
                }
                break;

            case SeriesTimer:
                navigationRepository.getValue().navigate(Destinations.INSTANCE.seriesTimerDetails(rowItem.getItemId(), ((SeriesTimerInfoDtoBaseRowItem) rowItem).getSeriesTimerInfo()));
                break;


            case GridButton:
                switch (((GridButtonBaseRowItem) rowItem).getGridButton().getId()) {
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
