package org.jellyfin.androidtv.util.apiclient;

import static org.jellyfin.androidtv.util.Utils.RUNTIME_TICKS_TO_MS;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.SessionRepository;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.ui.itemdetail.ItemListActivity;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.entities.LocationType;
import org.jellyfin.apiclient.model.entities.SortOrder;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.querying.EpisodeQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemFilter;
import org.jellyfin.apiclient.model.querying.ItemQuery;
import org.jellyfin.apiclient.model.querying.ItemSortBy;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.querying.NextUpQuery;
import org.jellyfin.apiclient.model.querying.SimilarItemsQuery;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import timber.log.Timber;

public class PlaybackHelper {
    private static final int ITEM_QUERY_LIMIT = 15; // use sane default for none music
    private static final int ITEM_QUERY_LIMIT_MUSIC = 150; // limit the number of items retrieved for playback

    public static void getItemsToPlay(final BaseItemDto mainItem, boolean allowIntros, final boolean shuffle, final Response<List<BaseItemDto>> outerResponse) {
        UUID userId = KoinJavaComponent.<SessionRepository>get(SessionRepository.class).getCurrentSession().getValue().getUserId();

        final List<BaseItemDto> items = new ArrayList<>();
        ItemQuery query = new ItemQuery();

        switch (mainItem.getBaseItemType()) {
            case Episode:
                items.add(mainItem);
                if (KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getMediaQueuingEnabled())) {
                    KoinJavaComponent.<MediaManager>get(MediaManager.class).setVideoQueueModified(false); // we are automatically creating new queue
                    //add subsequent episodes
                    if (mainItem.getSeriesId() != null && mainItem.getId() != null) {
                        EpisodeQuery episodeQuery = new EpisodeQuery();
                        if (mainItem.getSeasonId() != null) episodeQuery.setSeasonId(mainItem.getSeasonId());
                        episodeQuery.setSeriesId(mainItem.getSeriesId());
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
                                        if (foundMainItem) {
                                            if (!LocationType.Virtual.equals(item.getLocationType()) && numAdded < ITEM_QUERY_LIMIT) {
                                                items.add(item);
                                                numAdded++;
                                            } else {
                                                //stop adding when we hit a missing one or we have reached the limit
                                                break;
                                            }
                                        } else if (item.getId() != null && item.getId().equals(mainItem.getId())) {
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
            case Series:
            case Season:
            case BoxSet:
            case Folder:
                //get all videos
                query.setParentId(mainItem.getId());
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
                        Collections.addAll(items, response.getItems());
                        outerResponse.onResponse(items);
                    }
                });
                break;
            case MusicAlbum:
                query.setParentId(mainItem.getId());
            case MusicArtist:
                //get all songs
                query.setIsMissing(false);
                query.setIsVirtualUnaired(false);
                query.setMediaTypes(new String[]{"Audio"});
                query.setSortBy(mainItem.getBaseItemType() == BaseItemType.MusicArtist ?
                        new String[] {ItemSortBy.Album,ItemSortBy.SortName} :
                            new String[] {ItemSortBy.SortName});
                query.setRecursive(true);
                query.setLimit(ITEM_QUERY_LIMIT_MUSIC);
                query.setFields(new ItemFields[] {
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.Genres,
                        ItemFields.ChildCount
                });
                query.setUserId(userId.toString());
                if (mainItem.getBaseItemType() == BaseItemType.MusicArtist) {
                    query.setArtistIds(new String[]{mainItem.getId()});
                }
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        outerResponse.onResponse(Arrays.asList(response.getItems()));
                    }
                });
                break;
            case Playlist:
                if (mainItem.getId().equals(ItemListActivity.FAV_SONGS)) {
                    query.setFilters(new ItemFilter[] {ItemFilter.IsFavoriteOrLikes});
                } else {
                    query.setParentId(mainItem.getId());
                }
                query.setIsMissing(false);
                query.setIsVirtualUnaired(false);
                if (shuffle) query.setSortBy(new String[] {ItemSortBy.Random});
                query.setRecursive(true);
                query.setLimit(ITEM_QUERY_LIMIT_MUSIC);
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
                        outerResponse.onResponse(Arrays.asList(response.getItems()));
                    }
                });
                break;

            case Program:
                if (mainItem.getParentId() == null) {
                    outerResponse.onError(new Exception("No Channel ID"));
                    return;
                }

                //We retrieve the channel the program is on (which should be the program's parent)
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(mainItem.getParentId(), userId.toString(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        // fill in info about the specific program for display
                        response.setPremiereDate(mainItem.getPremiereDate());
                        response.setEndDate(mainItem.getEndDate());
                        response.setOfficialRating(mainItem.getOfficialRating());
                        response.setRunTimeTicks(mainItem.getRunTimeTicks());
                        items.add(response);
                        outerResponse.onResponse(items);
                    }

                    @Override
                    public void onError(Exception exception) {
                        super.onError(exception);
                    }
                });
                break;

            case TvChannel:
                // Retrieve full channel info for display
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetLiveTvChannelAsync(mainItem.getId(), userId.toString(), new Response<ChannelInfoDto>() {
                    @Override
                    public void onResponse(ChannelInfoDto response) {
                        // get current program info and fill it into our item
                        BaseItemDto program = response.getCurrentProgram();
                        if (program != null) {
                            mainItem.setPremiereDate(program.getStartDate());
                            mainItem.setEndDate(program.getEndDate());
                            mainItem.setOfficialRating(program.getOfficialRating());
                            mainItem.setRunTimeTicks(program.getRunTimeTicks());
                        }
                        addMainItem(mainItem, items, outerResponse);
                    }
                });
                break;

            default:
                if (allowIntros && !KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).useExternalPlayer(mainItem.getBaseItemType()) && KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getCinemaModeEnabled())) {
                    //Intros
                    KoinJavaComponent.<ApiClient>get(ApiClient.class).GetIntrosAsync(mainItem.getId(), userId.toString(), new Response<ItemsResult>() {
                        @Override
                        public void onResponse(ItemsResult response) {
                            if (response.getTotalRecordCount() > 0){
                                for (BaseItemDto intro : response.getItems()) {
                                    intro.setBaseItemType(BaseItemType.Trailer);
                                    items.add(intro);
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

    public static void playOrPlayNextUp(final BaseItemDto item, final Context activity) {
        if (item.getBaseItemType() == BaseItemType.Series || item.getBaseItemType() == BaseItemType.Season) {
            //play next up
            NextUpQuery nextUpQuery = new NextUpQuery();
            UUID userId = KoinJavaComponent.<SessionRepository>get(SessionRepository.class).getCurrentSession().getValue().getUserId();
            nextUpQuery.setUserId(userId.toString());
            nextUpQuery.setSeriesId(item.getSeriesId() != null ? item.getSeriesId() : item.getId());
            nextUpQuery.setLimit(1);
            KoinJavaComponent.<ApiClient>get(ApiClient.class).GetNextUpEpisodesAsync(nextUpQuery, new Response<ItemsResult>() {
                @Override
                public void onResponse(ItemsResult response) {
                    if (response.getItems().length > 0) {
                        retrieveAndPlay(response.getItems()[0].getId(), false, activity);
                    } else {
                        // try resume
                        ItemQuery query = new ItemQuery();
                        query.setUserId(userId.toString());
                        query.setParentId(item.getId());
                        query.setIsMissing(false);
                        query.setIsVirtualUnaired(false);
                        query.setIncludeItemTypes(new String[]{"Episode", "Video"});
                        query.setFilters(new ItemFilter[]{ItemFilter.IsResumable, ItemFilter.IsUnplayed});
                        query.setSortBy(new String[]{ItemSortBy.DatePlayed});
                        query.setSortOrder(SortOrder.Descending);
                        query.setRecursive(true);
                        query.setLimit(1);
                        KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemsAsync(query, new Response<ItemsResult>() {
                            @Override
                            public void onResponse(ItemsResult response) {
                                if (response.getItems().length > 0) {
                                    retrieveAndPlay(response.getItems()[0].getId(), false, activity);
                                } else {
                                    retrieveAndPlay(item.getId(), false, activity);
                                }
                            }
                        });
                    }
                }
            });
        } else {
            retrieveAndPlay(item.getId(), false, activity);
        }
    }

    public static void play(final BaseItemDto item, final int pos, final boolean shuffle, final Context activity) {
        PlaybackLauncher playbackLauncher = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class);
        if (playbackLauncher.interceptPlayRequest(activity, item)) return;

        getItemsToPlay(item, pos <= 0 && item.getBaseItemType() == BaseItemType.Movie, shuffle, new Response<List<BaseItemDto>>() {
            @Override
            public void onResponse(List<BaseItemDto> response) {
                switch (item.getBaseItemType()) {
                    case MusicAlbum:
                    case MusicArtist:
                        KoinJavaComponent.<MediaManager>get(MediaManager.class).playNow(activity, response, shuffle);
                        break;
                    case Playlist:
                        if ("Audio".equals(item.getMediaType())) {
                            KoinJavaComponent.<MediaManager>get(MediaManager.class).playNow(activity, response, shuffle);
                        } else {
                            BaseItemType itemType = response.size() > 0 ? response.get(0).getBaseItemType() : null;
                            Class newActivity = playbackLauncher.getPlaybackActivityClass(itemType);
                            Intent intent = new Intent(activity, newActivity);
                            KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentVideoQueue(response);
                            intent.putExtra("Position", Math.max(pos,0));
                            if (!(activity instanceof Activity)) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            }
                            activity.startActivity(intent);
                        }
                        break;
                    case Audio:
                        if (response.size() > 0) {
                            KoinJavaComponent.<MediaManager>get(MediaManager.class).playNow(activity, response.get(0));
                        }
                        break;

                    default:
                        Class newActivity = playbackLauncher.getPlaybackActivityClass(item.getBaseItemType());
                        Intent intent = new Intent(activity, newActivity);
                        KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentVideoQueue(response);
                        intent.putExtra("Position", Math.max(pos,0));
                        if (!(activity instanceof Activity)) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        }
                        activity.startActivity(intent);
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
                Long pos = position != null ? position / RUNTIME_TICKS_TO_MS : response.getUserData() != null ? (response.getUserData().getPlaybackPositionTicks() / RUNTIME_TICKS_TO_MS) - getResumePreroll() : 0;
                play(response, pos.intValue(), shuffle, activity);
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving item for playback");
                Utils.showToast(activity, R.string.msg_video_playback_error);
            }
        });
    }

    public static void playInstantMix(Context context, BaseItemDto item) {
        PlaybackLauncher playbackLauncher = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class);
        if (playbackLauncher.interceptPlayRequest(context, item)) return;

        String seedId = item.getId();
        getInstantMixAsync(seedId, new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (response.length > 0) {
                    KoinJavaComponent.<MediaManager>get(MediaManager.class).playNow(context, Arrays.asList(response), false);
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

    private static void addMainItem(BaseItemDto mainItem, final List<BaseItemDto> items, final Response<List<BaseItemDto>> outerResponse) {
        items.add(mainItem);
        if (mainItem.getPartCount() != null && mainItem.getPartCount() > 1) {
            // get additional parts
            UUID userId = KoinJavaComponent.<SessionRepository>get(SessionRepository.class).getCurrentSession().getValue().getUserId();
            KoinJavaComponent.<ApiClient>get(ApiClient.class).GetAdditionalParts(mainItem.getId(), userId.toString(), new Response<ItemsResult>() {
                @Override
                public void onResponse(ItemsResult response) {
                    Collections.addAll(items, response.getItems());
                    outerResponse.onResponse(items);
                }
            });
        } else {
            outerResponse.onResponse(items);
        }
    }
}
