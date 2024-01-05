package org.jellyfin.androidtv.util.apiclient;

import android.content.Context;
import android.provider.MediaStore;

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
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.sdk.compat.FakeBaseItem;
import org.jellyfin.androidtv.util.sdk.compat.JavaCompat;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.querying.EpisodeQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemFilter;
import org.jellyfin.apiclient.model.querying.ItemQuery;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.querying.SimilarItemsQuery;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.constant.ItemSortBy;
import org.jellyfin.sdk.model.constant.MediaType;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import timber.log.Timber;

public class PlaybackHelper {
    private static final int ITEM_QUERY_LIMIT = 150; // limit the number of items retrieved for playback

    public static void getItemsToPlay(final org.jellyfin.sdk.model.api.BaseItemDto mainItem, boolean allowIntros, final boolean shuffle, final Response<List<org.jellyfin.sdk.model.api.BaseItemDto>> outerResponse) {
        UUID userId = KoinJavaComponent.<SessionRepository>get(SessionRepository.class).getCurrentSession().getValue().getUserId();

        final List<org.jellyfin.sdk.model.api.BaseItemDto> items = new ArrayList<>();
        ItemQuery query = new ItemQuery();

        switch (mainItem.getType()) {
            case EPISODE:
                items.add(mainItem);
                if (KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getMediaQueuingEnabled())) {
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
                        KoinJavaComponent.<ApiClient>get(ApiClient.class).GetEpisodesAsync(episodeQuery, new Response<ItemsResult>() {
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
                query.setSortBy(new String[]{shuffle ? ItemSortBy.Random : ItemSortBy.SortName});
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
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemsAsync(query, new Response<ItemsResult>() {
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
                query.setMediaTypes(new String[]{MediaType.Audio});
                query.setSortBy(mainItem.getType() == BaseItemKind.MUSIC_ARTIST ?
                        new String[] {ItemSortBy.Album,ItemSortBy.SortName} :
                            new String[] {ItemSortBy.SortName});
                query.setRecursive(true);
                query.setLimit(ITEM_QUERY_LIMIT);
                query.setFields(new ItemFields[] {
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.Genres,
                        ItemFields.ChildCount
                });
                query.setUserId(userId.toString());
                query.setArtistIds(new String[]{mainItem.getId().toString()});
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        outerResponse.onResponse(JavaCompat.mapBaseItemArray(response.getItems()));
                    }
                });
                break;
            case PLAYLIST:
                if (mainItem.getId().equals(FakeBaseItem.INSTANCE.getFAV_SONGS_ID().toString())) {
                    query.setFilters(new ItemFilter[] {ItemFilter.IsFavoriteOrLikes});
                } else {
                    query.setParentId(mainItem.getId().toString());
                }
                query.setIsMissing(false);
                query.setIsVirtualUnaired(false);
                if (shuffle) query.setSortBy(new String[] {ItemSortBy.Random});
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
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemsAsync(query, new Response<ItemsResult>() {
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
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(mainItem.getParentId().toString(), userId.toString(), new Response<BaseItemDto>() {
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
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetLiveTvChannelAsync(mainItem.getId().toString(), userId.toString(), new Response<ChannelInfoDto>() {
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
                if (allowIntros && !KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).useExternalPlayer(mainItem.getType()) && KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getCinemaModeEnabled())) {
                    //Intros
                    KoinJavaComponent.<ApiClient>get(ApiClient.class).GetIntrosAsync(mainItem.getId().toString(), userId.toString(), new Response<ItemsResult>() {
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

    public static void play(final org.jellyfin.sdk.model.api.BaseItemDto item, final int pos, final boolean shuffle, final Context activity) {
        PlaybackLauncher playbackLauncher = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class);
        NavigationRepository navigationRepository = KoinJavaComponent.<NavigationRepository>get(NavigationRepository.class);
        if (playbackLauncher.interceptPlayRequest(activity, item)) return;

        getItemsToPlay(item, pos == 0 && item.getType() == BaseItemKind.MOVIE, shuffle, new Response<List<org.jellyfin.sdk.model.api.BaseItemDto>>() {
            @Override
            public void onResponse(List<org.jellyfin.sdk.model.api.BaseItemDto> response) {
                switch (item.getType()) {
                    case MUSIC_ALBUM:
                    case MUSIC_ARTIST:
                        KoinJavaComponent.<MediaManager>get(MediaManager.class).playNow(activity, response, shuffle);
                        break;
                    case PLAYLIST:
                        if (MediaType.Audio.equals(item.getMediaType())) {
                            KoinJavaComponent.<MediaManager>get(MediaManager.class).playNow(activity, response, shuffle);
                        } else {
                            BaseItemKind itemType = response.size() > 0 ? response.get(0).getType() : null;
                            KoinJavaComponent.<VideoQueueManager>get(VideoQueueManager.class).setCurrentVideoQueue(response);
                            Destination destination = playbackLauncher.getPlaybackDestination(itemType, pos);
                            navigationRepository.navigate(destination);
                        }
                        break;
                    case AUDIO:
                        if (response.size() > 0) {
                            KoinJavaComponent.<MediaManager>get(MediaManager.class).playNow(activity, response.get(0));
                        }
                        break;

                    default:
                        KoinJavaComponent.<VideoQueueManager>get(VideoQueueManager.class).setCurrentVideoQueue(response);
                        Destination destination = playbackLauncher.getPlaybackDestination(item.getType(), pos);

                        PlaybackController playbackController = KoinJavaComponent.<PlaybackControllerContainer>get(PlaybackControllerContainer.class).getPlaybackController();
                        navigationRepository.navigate(
                                destination,
                                playbackController != null && playbackController.hasFragment()
                        );
                }
            }
        });
    }

    public static void retrieveAndPlay(String id, boolean shuffle, Context activity) {
        retrieveAndPlay(id, shuffle, null, activity);
    }

    private static int getResumePreroll() {
        try {
            return Integer.parseInt(KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getResumeSubtractDuration())) * 1000;
        } catch (Exception e) {
            Timber.e(e, "Unable to parse resume preroll");
            return 0;
        }
    }

    public static void retrieveAndPlay(String id, final boolean shuffle, final Long position, final Context activity) {
        UUID userId = KoinJavaComponent.<SessionRepository>get(SessionRepository.class).getCurrentSession().getValue().getUserId();
        KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(id, userId.toString(), new Response<BaseItemDto>() {
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

    public static void playInstantMix(Context context, org.jellyfin.sdk.model.api.BaseItemDto item) {
        PlaybackLauncher playbackLauncher = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class);
        if (playbackLauncher.interceptPlayRequest(context, item)) return;

        String seedId = item.getId().toString();
        getInstantMixAsync(seedId, new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (response.length > 0) {
                    KoinJavaComponent.<MediaManager>get(MediaManager.class).playNow(context, JavaCompat.mapBaseItemArray(response), false);
                } else {
                    Utils.showToast(context, R.string.msg_no_playable_items);
                }
            }
        });
    }

    public static void getInstantMixAsync(String seedId, final Response<BaseItemDto[]> outerResponse) {
        UUID userId = KoinJavaComponent.<SessionRepository>get(SessionRepository.class).getCurrentSession().getValue().getUserId();
        SimilarItemsQuery query = new SimilarItemsQuery();
        query.setId(seedId);
        query.setUserId(userId.toString());
        query.setFields(new ItemFields[] {
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.Genres,
                ItemFields.ChildCount
        });
        KoinJavaComponent.<ApiClient>get(ApiClient.class).GetInstantMixFromItem(query, new Response<ItemsResult>() {
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

    private static void addMainItem(org.jellyfin.sdk.model.api.BaseItemDto mainItem, final List<org.jellyfin.sdk.model.api.BaseItemDto> items, final Response<List<org.jellyfin.sdk.model.api.BaseItemDto>> outerResponse) {
        items.add(mainItem);
        if (mainItem.getPartCount() != null && mainItem.getPartCount() > 1) {
            // get additional parts
            UUID userId = KoinJavaComponent.<SessionRepository>get(SessionRepository.class).getCurrentSession().getValue().getUserId();
            KoinJavaComponent.<ApiClient>get(ApiClient.class).GetAdditionalParts(mainItem.getId().toString(), userId.toString(), new Response<ItemsResult>() {
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
