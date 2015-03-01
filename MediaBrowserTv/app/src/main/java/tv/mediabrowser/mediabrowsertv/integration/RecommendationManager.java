package tv.mediabrowser.mediabrowsertv.integration;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import mediabrowser.model.dto.BaseItemDto;
import tv.mediabrowser.mediabrowsertv.R;
import tv.mediabrowser.mediabrowsertv.TvApp;
import tv.mediabrowser.mediabrowsertv.details.DetailsActivity;
import tv.mediabrowser.mediabrowsertv.util.Utils;

/**
 * Created by Eric on 3/1/2015.
 */
public class RecommendationManager {
    private final String REC_FILE_NAME = "tv.mediabrowser.recommentations.json";
    private final Integer MAX_TV_RECS = 3;
    private final Integer MAX_MOVIE_RECS = 4;

    private static RecommendationManager instance;

    private Recommendations mRecommendations;

    public RecommendationManager() {
        mRecommendations = loadRecs();
    }

    public static RecommendationManager getInstance() {
        if (instance == null) instance = new RecommendationManager();
        return instance;
    }

    private Recommendations loadRecs() {
        try {
            InputStream recFile = TvApp.getApplication().openFileInput(REC_FILE_NAME);
            String json = Utils.ReadStringFromFile(recFile);
            recFile.close();
            return (Recommendations) TvApp.getApplication().getSerializer().DeserializeFromString(json, Recommendations.class);
        } catch (IOException e) {
            // none saved
            return new Recommendations();
        }
    }

    private void saveRecs() {
        try {
            OutputStream recFile = TvApp.getApplication().openFileOutput(REC_FILE_NAME, Context.MODE_PRIVATE);
            recFile.write(TvApp.getApplication().getSerializer().SerializeToString(mRecommendations).getBytes());
            recFile.close();
        } catch (IOException e) {
            TvApp.getApplication().getLogger().ErrorException("Error saving recommendations",e);
        }

    }

    public void clearAll() {
        NotificationManager nm = (NotificationManager)
                TvApp.getApplication().getSystemService(Context.NOTIFICATION_SERVICE);

        nm.cancelAll();
        mRecommendations.setmMovieRecommendations(new ArrayList<Recommendation>());
        mRecommendations.setmTvRecommendations(new ArrayList<Recommendation>());
        saveRecs();
    }

    public boolean addRecommendation(BaseItemDto item, RecommendationType type) {

        //No need if we are already there
        if (mRecommendations.get(type, item.getId()) != null) return false;

        //Not so build one
        new AsyncRunner().execute(item, type);
        return true;

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
                    .setSmallIcon(R.drawable.mblogo100);

            Notification recommendation = builder.setId(rec.getRecId())
                    .setId(rec.getRecId())
                    .setPriority(0)
                    .setTitle(item.getName())
                    .setDescription(item.getOverview())
                    .setBitmap(Utils.getBitmapFromURL(Utils.getPrimaryImageUrl(item, TvApp.getApplication().getApiClient(), false, true, 300)))
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
        Intent detailsIntent = new Intent(TvApp.getApplication(), DetailsActivity.class);
        detailsIntent.putExtra("ItemId", item.getId());

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(TvApp.getApplication());
        stackBuilder.addParentStack(DetailsActivity.class);
        stackBuilder.addNextIntent(detailsIntent);
        // Ensure a unique PendingIntents, otherwise all recommendations end up with the same
        // PendingIntent
        detailsIntent.setAction(item.getId());

        PendingIntent intent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        return intent;
    }

}

