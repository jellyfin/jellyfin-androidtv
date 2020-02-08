package org.jellyfin.androidtv.util.apiclient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.details.ItemListActivity;
import org.jellyfin.androidtv.playback.MediaManager;
import org.jellyfin.androidtv.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.entities.LocationType;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemFilter;
import org.jellyfin.apiclient.model.querying.ItemQuery;
import org.jellyfin.apiclient.model.querying.ItemSortBy;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.querying.SimilarItemsQuery;

public class PlaybackHelper {
    public static void getItemsToPlay(final BaseItemDto mainItem, boolean allowIntros, final boolean shuffle, final Response<List<BaseItemDto>> outerResponse) {
        final List<BaseItemDto> items = new ArrayList<>();
        ItemQuery query = new ItemQuery();
        TvApp.getApplication().setPlayingIntros(false);

        switch (mainItem.getBaseItemType()) {
            case Episode:
                items.add(mainItem);
                if (TvApp.getApplication().getPrefs().getBoolean("pref_enable_tv_queuing", true)) {
                    MediaManager.setVideoQueueModified(false); // we are automatically creating new queue
                    //add subsequent episodes
                    if (mainItem.getSeasonId() != null && mainItem.getIndexNumber() != null) {
                        query.setParentId(mainItem.getSeasonId());
                        query.setIsVirtualUnaired(false);
                        query.setMinIndexNumber(mainItem.getIndexNumber() + 1);
                        query.setSortBy(new String[] {ItemSortBy.SortName});
                        query.setIncludeItemTypes(new String[]{"Episode"});
                        query.setFields(new ItemFields[] {ItemFields.MediaSources, ItemFields.MediaStreams, ItemFields.Path, ItemFields.Chapters, ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
                        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
                        TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
                            @Override
                            public void onResponse(ItemsResult response) {
                                if (response.getTotalRecordCount() > 0) {
                                    for (BaseItemDto item : response.getItems()) {
                                        if (item.getIndexNumber() > mainItem.getIndexNumber()) {
                                            if (!LocationType.Virtual.equals(item.getLocationType())) {
                                                items.add(item);
                                            } else {
                                                //stop adding when we hit a missing one
                                                break;
                                            }
                                        }
                                    }
                                }
                                outerResponse.onResponse(items);
                            }
                        });
                    } else {
                        TvApp.getApplication().getLogger().Info("Unable to add subsequent episodes due to lack of season or episode data.");
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
                query.setLimit(50); // guard against too many items
                query.setFields(new ItemFields[] {ItemFields.MediaSources, ItemFields.MediaStreams, ItemFields.Chapters, ItemFields.Path, ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
                query.setUserId(TvApp.getApplication().getCurrentUser().getId());
                TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        Collections.addAll(items, response.getItems());
                        outerResponse.onResponse(items);
                    }
                });
                break;
            case MusicAlbum:
            case MusicArtist:
                //get all songs
                query.setIsMissing(false);
                query.setIsVirtualUnaired(false);
                query.setMediaTypes(new String[]{"Audio"});
                query.setSortBy(shuffle ? new String[] {ItemSortBy.Random} : mainItem.getBaseItemType() == BaseItemType.MusicArtist ? new String[] {ItemSortBy.Album} : new String[] {ItemSortBy.SortName});
                query.setRecursive(true);
                query.setLimit(150); // guard against too many items
                query.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.Genres});
                query.setUserId(TvApp.getApplication().getCurrentUser().getId());
                query.setArtistIds(new String[]{mainItem.getId()});
                TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
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
                query.setLimit(150); // guard against too many items
                query.setFields(new ItemFields[] {ItemFields.MediaSources, ItemFields.MediaStreams, ItemFields.Chapters, ItemFields.Path, ItemFields.PrimaryImageAspectRatio});
                query.setUserId(TvApp.getApplication().getCurrentUser().getId());
                TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
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
                TvApp.getApplication().getApiClient().GetItemAsync(mainItem.getParentId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
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
                TvApp.getApplication().getApiClient().GetLiveTvChannelAsync(mainItem.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<ChannelInfoDto>() {
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
                if (allowIntros && !TvApp.getApplication().useExternalPlayer(mainItem.getBaseItemType()) && TvApp.getApplication().getPrefs().getBoolean("pref_enable_cinema_mode", true)) {
                    //Intros
                    TvApp.getApplication().getApiClient().GetIntrosAsync(mainItem.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<ItemsResult>() {
                        @Override
                        public void onResponse(ItemsResult response) {
                            if (response.getTotalRecordCount() > 0){
                                Collections.addAll(items, response.getItems());
                                TvApp.getApplication().getLogger().Info("%d intro items added for playback.", response.getTotalRecordCount());
                                TvApp.getApplication().setPlayingIntros(true);
                            } else {
                                TvApp.getApplication().setPlayingIntros(false);
                            }
                            //Finally, the main item including subsequent parts
                            addMainItem(mainItem, items, outerResponse);
                        }

                        @Override
                        public void onError(Exception exception) {
                            TvApp.getApplication().getLogger().ErrorException("Error retrieving intros", exception);
                            addMainItem(mainItem, items, outerResponse);
                        }
                    });

                } else {
                    addMainItem(mainItem, items, outerResponse);
                }
                break;
        }
    }

    public static void play(final BaseItemDto item, final int pos, final boolean shuffle, final Context activity) {
        getItemsToPlay(item, pos == 0 && item.getBaseItemType() == BaseItemType.Movie, shuffle, new Response<List<BaseItemDto>>() {
            @Override
            public void onResponse(List<BaseItemDto> response) {
                switch (item.getBaseItemType()) {
                    case MusicAlbum:
                    case MusicArtist:
                        MediaManager.playNow(response);
                        break;
                    case Playlist:
                        if ("Audio".equals(item.getMediaType())) {
                            MediaManager.playNow(response);

                        } else {
                            BaseItemType itemType = response.size() > 0 ? response.get(0).getBaseItemType() : null;
                            Intent intent = new Intent(activity, TvApp.getApplication().getPlaybackActivityClass(itemType));
                            MediaManager.setCurrentVideoQueue(response);
                            intent.putExtra("Position", pos);
                            if (!(activity instanceof Activity))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.startActivity(intent);
                        }
                        break;
                    case Audio:
                        if (response.size() > 0) {
                            MediaManager.playNow(response.get(0));
                        }
                        break;

                    default:
                        Intent intent = new Intent(activity, TvApp.getApplication().getPlaybackActivityClass(item.getBaseItemType()));
                        MediaManager.setCurrentVideoQueue(response);
                        intent.putExtra("Position", pos);
                        if (!(activity instanceof Activity))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                }
            }
        });
    }

    public static void retrieveAndPlay(String id, boolean shuffle, Context activity) {
        retrieveAndPlay(id, shuffle, null, activity);
    }

    public static void retrieveAndPlay(String id, final boolean shuffle, final Long position, final Context activity) {
        TvApp.getApplication().getApiClient().GetItemAsync(id, TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
            @Override
            public void onResponse(BaseItemDto response) {
                Long pos = position != null ? position / 10000 : response.getUserData() != null ? (response.getUserData().getPlaybackPositionTicks() / 10000) - TvApp.getApplication().getResumePreroll() : 0;
                play(response, pos.intValue(), shuffle, activity);
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving item for playback", exception);
                Utils.showToast(activity, R.string.msg_video_playback_error);
            }
        });
    }

    public static void playInstantMix(String seedId) {
        getInstantMixAsync(seedId, new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (response.length > 0) {
                    MediaManager.playNow(Arrays.asList(response));
                } else {
                    Utils.showToast(TvApp.getApplication(), R.string.msg_no_playable_items);
                }
            }
        });
    }

    public static void getInstantMixAsync(String seedId, final Response<BaseItemDto[]> outerResponse) {
        SimilarItemsQuery query = new SimilarItemsQuery();
        query.setId(seedId);
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        query.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.Genres});
        TvApp.getApplication().getApiClient().GetInstantMixFromItem(query, new Response<ItemsResult>() {
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
            TvApp.getApplication().getApiClient().GetAdditionalParts(mainItem.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<ItemsResult>() {
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
