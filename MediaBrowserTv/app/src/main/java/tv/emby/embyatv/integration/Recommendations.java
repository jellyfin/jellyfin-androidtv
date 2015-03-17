package tv.emby.embyatv.integration;

import android.app.NotificationManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import tv.emby.embyatv.TvApp;

/**
 * Created by Eric on 3/1/2015.
 */
public class Recommendations {

    private String serverId;
    private String userId;
    private List<Recommendation> tvRecommendations = new ArrayList<>();
    private List<Recommendation> movieRecommendations = new ArrayList<>();

    public Recommendations(String serverId, String userId) {
        this.userId = userId;
        this.serverId = serverId;
    }

    public List<Recommendation> getTvRecommendations() {
        return tvRecommendations;
    }

    public void setTvRecommendations(List<Recommendation> tvRecommendations) {
        this.tvRecommendations = tvRecommendations;
    }

    public List<Recommendation> getMovieRecommendations() {
        return movieRecommendations;
    }

    public void setMovieRecommendations(List<Recommendation> movieRecommendations) {
        this.movieRecommendations = movieRecommendations;
    }

    public Recommendation get(RecommendationType type, String id) {
        Recommendation compare = new Recommendation(type, id);
        switch (type) {

            case Movie:
                return movieRecommendations.contains(compare) ? movieRecommendations.get(movieRecommendations.indexOf(compare)) : null;
            case Tv:
                return tvRecommendations.contains(compare) ? tvRecommendations.get(tvRecommendations.indexOf(compare)) : null;
        }

        return null;
    }

    public boolean add(Recommendation rec) {
        switch (rec.getType()) {

            case Movie:
                movieRecommendations.add(rec);
                break;
            case Tv:
                tvRecommendations.add(rec);
                break;
        }

        return true;
    }

    public Integer getRecId(RecommendationType type, int max) {
        switch (type) {
            case Movie:
                if (movieRecommendations.size() < max) return 100 + movieRecommendations.size()+1;
                return replaceOldest(type);
            case Tv:
                if (tvRecommendations.size() < max) return 200 + tvRecommendations.size()+1;
                return replaceOldest(type);
        }

        throw new IllegalArgumentException("type");
    }

    private Integer replaceOldest(RecommendationType type) {
        List<Recommendation> list = type == RecommendationType.Movie ? movieRecommendations : tvRecommendations;

        Recommendation oldest = Collections.min(list, new Comparator<Recommendation>() {
            @Override
            public int compare(Recommendation lhs, Recommendation rhs) {
                return Long.compare(lhs.getDateAdded(), rhs.getDateAdded());
            }
        });

        list.remove(oldest);
        ((NotificationManager) TvApp.getApplication().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(oldest.getRecId());
        return oldest.getRecId();
    }

    public boolean remove(RecommendationType type, String id) {
        List<Recommendation> list = type == RecommendationType.Movie ? movieRecommendations : tvRecommendations;

        Recommendation existing = get(type, id);

        if (existing != null) {
            list.remove(existing);
            ((NotificationManager) TvApp.getApplication().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(existing.getRecId());
            return true;
        }

        return false;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
}

