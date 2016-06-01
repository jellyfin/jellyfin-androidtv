package tv.emby.embyatv.integration;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.NextUpQuery;
import mediabrowser.model.querying.SimilarItemsQuery;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.browsing.MainActivity;
import tv.emby.embyatv.querying.StdItemQuery;
import tv.emby.embyatv.startup.StartupActivity;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 3/1/2015.
 */
public class RecommendationManager {
    private final String REC_FILE_NAME = "tv.mediabrowser.recommentations.json";
    private final Integer MAX_TV_RECS = 3;
    private final Integer MAX_MOVIE_RECS = 4;

    private boolean isEnabled;

    private static RecommendationManager instance;

    private Recommendations mRecommendations;

    public RecommendationManager() {
        isEnabled = Build.VERSION.SDK_INT >= 21;
        mRecommendations = loadRecs();
        if (isEnabled) {
            validate();
        } else {
            TvApp.getApplication().getLogger().Info("Recommendations not enabled on this device");
        }
    }

    public static RecommendationManager getInstance() {
        if (instance == null) instance = new RecommendationManager();
        return instance;
    }

    public static void init() {
        if (instance == null) {
            getInstance();
        } else {
            instance.validate();
        }
    }

    private Recommendations loadRecs() {
        if (isEnabled) {
            try {
                InputStream recFile = TvApp.getApplication().openFileInput(REC_FILE_NAME);
                String json = Utils.ReadStringFromFile(recFile);
                recFile.close();
                return (Recommendations) TvApp.getApplication().getSerializer().DeserializeFromString(json, Recommendations.class);
            } catch (IOException e) {
                // none saved
                return new Recommendations(TvApp.getApplication().getApiClient().getServerInfo().getId(), TvApp.getApplication().getCurrentUser().getId());
            }
        } else {
            return new Recommendations(TvApp.getApplication().getApiClient().getServerInfo().getId(), TvApp.getApplication().getCurrentUser().getId());
        }
    }

    public boolean validate() {
        if (isEnabled) {
            //Now validate that these are for this server and user.
            if (mRecommendations == null || mRecommendations.getServerId() == null || TvApp.getApplication().getApiClient().getServerInfo() == null || !mRecommendations.getServerId().equals(TvApp.getApplication().getApiClient().getServerInfo().getId())
                  || mRecommendations.getUserId() == null || TvApp.getApplication().getCurrentUser() == null || !mRecommendations.getUserId().equals(TvApp.getApplication().getCurrentUser().getId())) {
                //Nope - clear them out and start over for this user
                clearAll();
                createAll();
                TvApp.getApplication().getLogger().Info("Recommendations re-set for user "+TvApp.getApplication().getCurrentUser().getName());
                return false;
            }

            createAll();
            TvApp.getApplication().getLogger().Info("Recommendations re-created for user "+TvApp.getApplication().getCurrentUser().getName());

        }

        return true;
    }

    private void saveRecs() {
        if (isEnabled) {
            try {
                OutputStream recFile = TvApp.getApplication().openFileOutput(REC_FILE_NAME, Context.MODE_PRIVATE);
                recFile.write(TvApp.getApplication().getSerializer().SerializeToString(mRecommendations).getBytes());
                recFile.close();
            } catch (IOException e) {
                TvApp.getApplication().getLogger().ErrorException("Error saving recommendations",e);
            }
        }

    }

    public void clearAll() {
        NotificationManager nm = (NotificationManager)
                TvApp.getApplication().getSystemService(Context.NOTIFICATION_SERVICE);

        nm.cancelAll();
        mRecommendations = new Recommendations(TvApp.getApplication().getApiClient().getServerInfo().getId(), TvApp.getApplication().getCurrentUser().getId());
        saveRecs();
    }

    public void createAll() {
        if (isEnabled) {
            //Create recs for next up tv and movies
            NextUpQuery nextUpQuery = new NextUpQuery();
            nextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
            nextUpQuery.setLimit(MAX_TV_RECS);
            nextUpQuery.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio});
            TvApp.getApplication().getApiClient().GetNextUpEpisodesAsync(nextUpQuery, new Response<ItemsResult>() {
                @Override
                public void onResponse(ItemsResult response) {
                    if (response.getTotalRecordCount() > 0) {
                        for (BaseItemDto episode : response.getItems()) {
                            if (episode.getLocationType() != LocationType.Virtual) addRecommendation(episode, RecommendationType.Tv);
                        }
                    }
                }
            });

            //First try for resumables
            StdItemQuery resumeMovies = new StdItemQuery();
            resumeMovies.setIncludeItemTypes(new String[]{"Movie"});
            resumeMovies.setRecursive(true);
            resumeMovies.setLimit(2);
            resumeMovies.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
            resumeMovies.setSortBy(new String[]{ItemSortBy.DatePlayed});
            resumeMovies.setSortOrder(SortOrder.Descending);
            TvApp.getApplication().getApiClient().GetItemsAsync(resumeMovies, new Response<ItemsResult>() {
                @Override
                public void onResponse(ItemsResult response) {
                    int movieItems = 0;
                    if (response.getTotalRecordCount() > 0) {
                        for (BaseItemDto movie : response.getItems()) {
                            recommend(movie.getId());
                            movieItems++;
                        }
                    }
                    if (movieItems < MAX_MOVIE_RECS) {
                        //Now fill in with latest movies
                        StdItemQuery suggMovies = new StdItemQuery();
                        suggMovies.setIncludeItemTypes(new String[]{"Movie"});
                        suggMovies.setRecursive(true);
                        suggMovies.setLimit(MAX_MOVIE_RECS - movieItems);
                        suggMovies.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
                        suggMovies.setSortBy(new String[]{ItemSortBy.DateCreated});
                        suggMovies.setSortOrder(SortOrder.Descending);
                        TvApp.getApplication().getApiClient().GetItemsAsync(suggMovies, new Response<ItemsResult>() {
                            @Override
                            public void onResponse(ItemsResult suggResponse) {
                                if (suggResponse.getTotalRecordCount() > 0) {
                                    for (BaseItemDto movie : suggResponse.getItems()) {
                                        addRecommendation(movie, RecommendationType.Movie);
                                    }
                                }
                            }
                        });
                    }
                }
            });
        }

    }

    public void recommend(final String itemId) {
        if (isEnabled) {
            if (itemId == null) {
                TvApp.getApplication().getLogger().Error("Attempt to recommend null Item");
                return;
            }
            //No matter what it is, if it is resumable, recommend this item (need to re-retrieve for current user data)
            TvApp.getApplication().getApiClient().GetItemAsync(itemId, TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                @Override
                public void onResponse(BaseItemDto response) {
                    if (response == null) {
                        TvApp.getApplication().getLogger().Error("No item found with ID: "+itemId);
                        return;
                    }
                    if (response.getCanResume()) {
                        addRecommendation(response, RecommendationType.Movie);
                    } else {
                        switch (response.getType()) {
                            case "Movie":
                                //First remove us if we were a recommendation
                                mRecommendations.remove(RecommendationType.Movie, response.getId());

                                //Suggest a similar movie
                                SimilarItemsQuery similar = new SimilarItemsQuery();
                                similar.setId(response.getId());
                                similar.setLimit(1);
                                similar.setUserId(TvApp.getApplication().getCurrentUser().getId());
                                TvApp.getApplication().getApiClient().GetSimilarItems(similar, new Response<ItemsResult>() {
                                    @Override
                                    public void onResponse(ItemsResult similarResponse) {
                                        if (similarResponse.getTotalRecordCount() > 0) {
                                            addRecommendation(similarResponse.getItems()[0], RecommendationType.Movie);
                                        }
                                    }

                                    @Override
                                    public void onError(Exception exception) {
                                        TvApp.getApplication().getLogger().ErrorException("Error retrieving item for recommendation", exception);
                                    }
                                });
                                break;
                            case "Episode":
                                //First remove us if we were a recommendation
                                mRecommendations.remove(RecommendationType.Tv, response.getId());

                                //Suggest the next up episode
                                NextUpQuery next = new NextUpQuery();
                                next.setSeriesId(response.getSeriesId());
                                next.setUserId(TvApp.getApplication().getCurrentUser().getId());
                                next.setLimit(1);
                                TvApp.getApplication().getApiClient().GetNextUpEpisodesAsync(next, new Response<ItemsResult>() {
                                    @Override
                                    public void onResponse(ItemsResult nextResponse) {
                                        if (nextResponse.getTotalRecordCount() > 0 && nextResponse.getItems()[0].getLocationType() != LocationType.Virtual) {
                                            addRecommendation(nextResponse.getItems()[0], RecommendationType.Tv);
                                        }
                                    }

                                    @Override
                                    public void onError(Exception exception) {
                                        TvApp.getApplication().getLogger().ErrorException("Error retrieving item for recommendation", exception);
                                    }
                                });
                                break;
                        }

                    }
                }

                @Override
                public void onError(Exception exception) {
                    TvApp.getApplication().getLogger().ErrorException("Error retrieving item for recommendation", exception);
                }
            });
        }

    }

    public boolean addRecommendation(BaseItemDto item, RecommendationType type) {
        if (isEnabled) {
            //No need if we are already there
            if (mRecommendations.get(type, item.getId()) != null) return false;

            //Not so build one
            new AsyncRunner().execute(item, type);
            return true;
        } else {
            return false;
        }

    }

    private class AsyncRunner extends AsyncTask<Object, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(Object... params) {
            BaseItemDto item = (BaseItemDto) params[0];
            RecommendationType type = (RecommendationType) params[1];
            Recommendation rec = new Recommendation(type, item.getId());
            rec.setRecId(mRecommendations.getRecId(type, type == RecommendationType.Movie ? MAX_MOVIE_RECS : MAX_TV_RECS));
            RecommendationBuilder builder = new RecommendationBuilder()
                    .setContext(TvApp.getApplication())
                    .setSmallIcon(R.drawable.logoicon114);

            Notification recommendation = builder
                    .setId(rec.getRecId())
                    .setPriority(0)
                    .setTitle(item.getName())
                    .setDescription(item.getOverview())
                    .setBitmap(Utils.getBitmapFromURL(Utils.getPrimaryImageUrl(item, TvApp.getApplication().getApiClient(), false, true, 300)))
                    .setBackground(Utils.getBackdropImageUrl(item, TvApp.getApplication().getApiClient(), true))
                    .setIntent(buildPendingIntent(item))
                    .build();

            mRecommendations.add(rec);
            saveRecs();

            NotificationManager notificationManager = (NotificationManager)
                    TvApp.getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(rec.getRecId(), recommendation);

            return true;
        }
    }

    private PendingIntent buildPendingIntent(BaseItemDto item) {
        Intent directIntent = new Intent(TvApp.getApplication(), StartupActivity.class);
        directIntent.putExtra("ItemId", item.getId());
        directIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(TvApp.getApplication());
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(directIntent);
        // Ensure a unique PendingIntents, otherwise all recommendations end up with the same
        // PendingIntent
        directIntent.setAction(item.getId());

        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}

