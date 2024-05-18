package org.jellyfin.androidtv.util.apiclient;

import android.content.Context;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.SessionRepository;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.ui.navigation.Destination;
import org.jellyfin.androidtv.ui.navigation.NavigationRepository;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer;
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher;
import org.jellyfin.androidtv.ui.playback.VideoQueueManager;
import org.jellyfin.androidtv.util.PlaybackHelper;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.sdk.compat.JavaCompat;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.querying.EpisodeQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemQuery;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.querying.SimilarItemsQuery;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.ItemSortBy;
import org.jellyfin.sdk.model.api.MediaType;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import kotlin.Lazy;
import timber.log.Timber;

public class ApiClientPlaybackHelper implements PlaybackHelper {
    private static final int ITEM_QUERY_LIMIT = 150; // limit the number of items retrieved for playback

    private Lazy<SessionRepository> sessionRepository = KoinJavaComponent.<SessionRepository>inject(SessionRepository.class);
    private Lazy<UserPreferences> userPreferences = KoinJavaComponent.<UserPreferences>inject(UserPreferences.class);
    private Lazy<ApiClient> apiClient = KoinJavaComponent.<ApiClient>inject(ApiClient.class);
    private Lazy<PlaybackLauncher> playbackLauncher = KoinJavaComponent.<PlaybackLauncher>inject(PlaybackLauncher.class);
    private Lazy<MediaManager> mediaManager = KoinJavaComponent.<MediaManager>inject(MediaManager.class);
    private Lazy<VideoQueueManager> videoQueueManager = KoinJavaComponent.<VideoQueueManager>inject(VideoQueueManager.class);
    private Lazy<NavigationRepository> navigationRepository = KoinJavaComponent.<NavigationRepository>inject(NavigationRepository.class);
    private Lazy<PlaybackControllerContainer> playbackControllerContainer = KoinJavaComponent.<PlaybackControllerContainer>inject(PlaybackControllerContainer.class);

    @Override
    public void getItemsToPlay(Context context, final org.jellyfin.sdk.model.api.BaseItemDto mainItem, boolean allowIntros, final boolean shuffle, final Response<List<org.jellyfin.sdk.model.api.BaseItemDto>> outerResponse) {
        UUID userId = sessionRepository.getValue().getCurrentSession().getValue().getUserId();

        final List<org.jellyfin.sdk.model.api.BaseItemDto> items = new ArrayList<>();
        ItemQuery query = new ItemQuery();

        switch (mainItem.getType()) {
            case EPISODE:
                items.add(mainItem);
                if (userPreferences.getValue().get(UserPreferences.Companion.getMediaQueuingEnabled())) {
                    //add subsequent episodes
                    if (mainItem.getSeriesId() != null && mainItem.getId() != null) {
                        EpisodeQuery episodeQuery = new EpisodeQuery();
                        if (mainItem.getSeasonId() != null) episodeQuery.setSeasonId(mainItem.getSeasonId().toString());
                        episodeQuery.setSeriesId(mainItem.getSeriesId().toString());
                        episodeQuery.setUserId(userId.toString());
                        episodeQuery.setIsVirtualUnaired(false);
                        episodeQuery.setIsMissing(false);
                        episodeQuery.setFields(new ItemFields[] {
                                ItemFields.MediaSources,
                                ItemFields.MediaStreams,
                                ItemFields.Path,
                                ItemFields.Chapters,
                                ItemFields.Overview,
                                ItemFields.PrimaryImageAspectRatio,
                                ItemFields.ChildCount
                        });
                        apiClient.getValue().GetEpisodesAsync(episodeQuery, new Response<ItemsResult>() {
                            @Override
                            public void onResponse(ItemsResult response) {
                                if (response.getTotalRecordCount() > 0) {
                                    // TODO: Finding the main item should be possible in the query using StartItemId, but it is not currently supported.
                                    // With StartItemId added, the limit could also be included in the query.
                                    boolean foundMainItem = false;
                                    int numAdded = 0;
                                    for (BaseItemDto item : response.getItems()) {
                                        org.jellyfin.sdk.model.api.BaseItemDto baseItem = ModelCompat.asSdk(item);
                                        if (foundMainItem) {
                                            if (!baseItem.getLocationType().equals(org.jellyfin.sdk.model.api.LocationType.VIRTUAL) && numAdded < ITEM_QUERY_LIMIT) {
                                                items.add(baseItem);
                                                numAdded++;
                                            } else {
                                                //stop adding when we hit a missing one or we have reached the limit
                                                break;
                                            }
                                        } else if (baseItem.getId() != null && baseItem.getId().equals(mainItem.getId())) {
                                            foundMainItem = true;
                                        }
                                    }
                                }
                                outerResponse.onResponse(items);
                            }
                        });
                    } else {
                        Timber.i("Unable to add subsequent episodes due to lack of series or episode data.");
                        outerResponse.onResponse(items);
                    }
                } else {
                    outerResponse.onResponse(items);
                }
                break;
            case SERIES:
            case SEASON:
            case BOX_SET:
            case FOLDER:
                //get all videos
                query.setParentId(mainItem.getId().toString());
                query.setIsMissing(false);
                query.setIsVirtualUnaired(false);
                query.setIncludeItemTypes(new String[]{"Episode", "Movie", "Video"});
                query.setSortBy(new String[]{shuffle ? ItemSortBy.RANDOM.getSerialName() : ItemSortBy.SORT_NAME.getSerialName()});
                query.setRecursive(true);
                query.setLimit(ITEM_QUERY_LIMIT);
                query.setFields(new ItemFields[] {
                        ItemFields.MediaSources,
                        ItemFields.MediaStreams,
                        ItemFields.Chapters,
                        ItemFields.Path,
                        ItemFields.Overview,
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                query.setUserId(userId.toString());
                apiClient.getValue().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        for (BaseItemDto item : response.getItems()) {
                            items.add(ModelCompat.asSdk(item));
                        }
                        outerResponse.onResponse(items);
                    }
                });
                break;
            case MUSIC_ALBUM:
            case MUSIC_ARTIST:
                //get all songs
                query.setIsMissing(false);
                query.setIsVirtualUnaired(false);
                query.setMediaTypes(new String[]{MediaType.AUDIO.getSerialName()});
                query.setSortBy(mainItem.getType() == BaseItemKind.MUSIC_ARTIST ?
                        new String[] {ItemSortBy.ALBUM_ARTIST.getSerialName(),ItemSortBy.SORT_NAME.getSerialName()} :
                            new String[] {ItemSortBy.SORT_NAME.getSerialName()});
                query.setRecursive(true);
                query.setLimit(ITEM_QUERY_LIMIT);
                query.setFields(new ItemFields[] {
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.Genres,
                        ItemFields.ChildCount
                });
                query.setUserId(userId.toString());
                query.setArtistIds(new String[]{mainItem.getId().toString()});
                apiClient.getValue().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        outerResponse.onResponse(JavaCompat.mapBaseItemArray(response.getItems()));
                    }
                });
                break;
            case PLAYLIST:
                query.setParentId(mainItem.getId().toString());
                query.setIsMissing(false);
                query.setIsVirtualUnaired(false);
                if (shuffle) query.setSortBy(new String[] {ItemSortBy.RANDOM.getSerialName()});
                query.setRecursive(true);
                query.setLimit(ITEM_QUERY_LIMIT);
                query.setFields(new ItemFields[] {
                        ItemFields.MediaSources,
                        ItemFields.MediaStreams,
                        ItemFields.Chapters,
                        ItemFields.Path,
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                query.setUserId(userId.toString());
                apiClient.getValue().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        outerResponse.onResponse(JavaCompat.mapBaseItemArray(response.getItems()));
                    }
                });
                break;

            case PROGRAM:
                if (mainItem.getParentId() == null) {
                    outerResponse.onError(new Exception("No Channel ID"));
                    return;
                }

                //We retrieve the channel the program is on (which should be the program's parent)
                apiClient.getValue().GetItemAsync(mainItem.getParentId().toString(), userId.toString(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        // fill in info about the specific program for display
                        response.setPremiereDate(TimeUtils.getDate(mainItem.getPremiereDate()));
                        response.setEndDate(TimeUtils.getDate(mainItem.getEndDate()));
                        response.setOfficialRating(mainItem.getOfficialRating());
                        response.setRunTimeTicks(mainItem.getRunTimeTicks());
                        items.add(ModelCompat.asSdk(response));
                        outerResponse.onResponse(items);
                    }

                    @Override
                    public void onError(Exception exception) {
                        super.onError(exception);
                    }
                });
                break;

            case TV_CHANNEL:
                // Retrieve full channel info for display
                apiClient.getValue().GetLiveTvChannelAsync(mainItem.getId().toString(), userId.toString(), new Response<ChannelInfoDto>() {
                    @Override
                    public void onResponse(ChannelInfoDto response) {
                        // get current program info and fill it into our item
                        org.jellyfin.sdk.model.api.BaseItemDto program = response.getCurrentProgram() != null ? ModelCompat.asSdk(response.getCurrentProgram()) : null;
                        org.jellyfin.sdk.model.api.BaseItemDto item = mainItem;
                        if (program != null) {
                            item = JavaCompat.copyWithDates(
                                mainItem,
                                program.getStartDate(),
                                program.getEndDate(),
                                program.getOfficialRating(),
                                program.getRunTimeTicks()
                            );
                        }
                        addMainItem(item, items, outerResponse);
                    }
                });
                break;

            default:
                if (allowIntros && !playbackLauncher.getValue().useExternalPlayer(mainItem.getType()) && userPreferences.getValue().get(UserPreferences.Companion.getCinemaModeEnabled())) {
                    //Intros
                    apiClient.getValue().GetIntrosAsync(mainItem.getId().toString(), userId.toString(), new Response<ItemsResult>() {
                        @Override
                        public void onResponse(ItemsResult response) {
                            if (response.getTotalRecordCount() > 0){

                                for (BaseItemDto intro : response.getItems()) {
                                    intro.setBaseItemType(BaseItemType.Trailer);
                                    items.add(ModelCompat.asSdk(intro));
                                }
                                Timber.i("%d intro items added for playback.", response.getTotalRecordCount());
                            }
                            //Finally, the main item including subsequent parts
                            addMainItem(mainItem, items, outerResponse);
                        }

                        @Override
                        public void onError(Exception exception) {
                            Timber.e(exception, "Error retrieving intros");
                            addMainItem(mainItem, items, outerResponse);
                        }
                    });

                } else {
                    addMainItem(mainItem, items, outerResponse);
                }
                break;
        }
    }

    private void play(final org.jellyfin.sdk.model.api.BaseItemDto item, final int pos, final boolean shuffle, final Context activity) {
        getItemsToPlay(activity, item, pos == 0 && item.getType() == BaseItemKind.MOVIE, shuffle, new Response<List<org.jellyfin.sdk.model.api.BaseItemDto>>() {
            @Override
            public void onResponse(List<org.jellyfin.sdk.model.api.BaseItemDto> response) {
                switch (item.getType()) {
                    case MUSIC_ALBUM:
                    case MUSIC_ARTIST:
                        mediaManager.getValue().playNow(activity, response, 0, shuffle);
                        break;
                    case PLAYLIST:
                        if (MediaType.AUDIO.equals(item.getMediaType())) {
                            mediaManager.getValue().playNow(activity, response, 0, shuffle);
                        } else {
                            BaseItemKind itemType = response.size() > 0 ? response.get(0).getType() : null;
                            videoQueueManager.getValue().setCurrentVideoQueue(response);
                            Destination destination = playbackLauncher.getValue().getPlaybackDestination(itemType, pos);
                            navigationRepository.getValue().navigate(destination);
                        }
                        break;
                    case AUDIO:
                        if (response.size() > 0) {
                            mediaManager.getValue().playNow(activity, Arrays.asList(response.get(0)), 0, false);
                        }
                        break;

                    default:
                        videoQueueManager.getValue().setCurrentVideoQueue(response);
                        Destination destination = playbackLauncher.getValue().getPlaybackDestination(item.getType(), pos);

                        PlaybackController playbackController = playbackControllerContainer.getValue().getPlaybackController();
                        navigationRepository.getValue().navigate(
                                destination,
                                playbackController != null && playbackController.hasFragment()
                        );
                }
            }
        });
    }

    @Override
    public void retrieveAndPlay(UUID id, boolean shuffle, Context activity) {
        retrieveAndPlay(id, shuffle, null, activity);
    }

    private int getResumePreroll() {
        try {
            return Integer.parseInt(userPreferences.getValue().get(UserPreferences.Companion.getResumeSubtractDuration())) * 1000;
        } catch (Exception e) {
            Timber.e(e, "Unable to parse resume preroll");
            return 0;
        }
    }

    @Override
    public void retrieveAndPlay(UUID id, final boolean shuffle, final Long position, final Context activity) {
        UUID userId = sessionRepository.getValue().getCurrentSession().getValue().getUserId();
        apiClient.getValue().GetItemAsync(id.toString(), userId.toString(), new Response<BaseItemDto>() {
            @Override
            public void onResponse(BaseItemDto response) {
                Long pos = position != null ? position / 10000 : response.getUserData() != null ? (response.getUserData().getPlaybackPositionTicks() / 10000) - getResumePreroll() : 0;
                play(ModelCompat.asSdk(response), pos.intValue(), shuffle, activity);
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving item for playback");
                Utils.showToast(activity, R.string.msg_video_playback_error);
            }
        });
    }

    @Override
    public void playInstantMix(Context context, org.jellyfin.sdk.model.api.BaseItemDto item) {
        getInstantMixAsync(item.getId(), new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (response.length > 0) {
                    mediaManager.getValue().playNow(context, JavaCompat.mapBaseItemArray(response), 0, false);
                } else {
                    Utils.showToast(context, R.string.msg_no_playable_items);
                }
            }
        });
    }

    private void getInstantMixAsync(UUID seedId, final Response<BaseItemDto[]> outerResponse) {
        UUID userId = sessionRepository.getValue().getCurrentSession().getValue().getUserId();
        SimilarItemsQuery query = new SimilarItemsQuery();
        query.setId(seedId.toString());
        query.setUserId(userId.toString());
        query.setFields(new ItemFields[] {
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.Genres,
                ItemFields.ChildCount
        });
        apiClient.getValue().GetInstantMixFromItem(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                outerResponse.onResponse(response.getItems());
            }

            @Override
            public void onError(Exception exception) {
                outerResponse.onError(exception);
            }
        });
    }

    private void addMainItem(org.jellyfin.sdk.model.api.BaseItemDto mainItem, final List<org.jellyfin.sdk.model.api.BaseItemDto> items, final Response<List<org.jellyfin.sdk.model.api.BaseItemDto>> outerResponse) {
        items.add(mainItem);
        if (mainItem.getPartCount() != null && mainItem.getPartCount() > 1) {
            // get additional parts
            UUID userId = sessionRepository.getValue().getCurrentSession().getValue().getUserId();
            apiClient.getValue().GetAdditionalParts(mainItem.getId().toString(), userId.toString(), new Response<ItemsResult>() {
                @Override
                public void onResponse(ItemsResult response) {
                    for (BaseItemDto item : response.getItems()) {
                        items.add(ModelCompat.asSdk(item));
                    }
                    outerResponse.onResponse(items);
                }
            });
        } else {
            outerResponse.onResponse(items);
        }
    }
}
