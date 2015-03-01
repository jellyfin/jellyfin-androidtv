package tv.mediabrowser.mediabrowsertv.integration;

import android.app.NotificationManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import tv.mediabrowser.mediabrowsertv.TvApp;

/**
 * Created by Eric on 3/1/2015.
 */
public class Recommendations {

    private List<Recommendation> mTvRecommendations = new ArrayList<>();
    private List<Recommendation> mMovieRecommendations = new ArrayList<>();

    public List<Recommendation> getmTvRecommendations() {
        return mTvRecommendations;
    }

    public void setmTvRecommendations(List<Recommendation> mTvRecommendations) {
        this.mTvRecommendations = mTvRecommendations;
    }

    public List<Recommendation> getmMovieRecommendations() {
        return mMovieRecommendations;
    }

    public void setmMovieRecommendations(List<Recommendation> mMovieRecommendations) {
        this.mMovieRecommendations = mMovieRecommendations;
    }

    public Recommendation get(RecommendationType type, String id) {
        Recommendation compare = new Recommendation(type, id);
        switch (type) {

            case Movie:
                return mMovieRecommendations.contains(compare) ? mMovieRecommendations.get(mMovieRecommendations.indexOf(compare)) : null;
            case Tv:
                return mTvRecommendations.contains(compare) ? mTvRecommendations.get(mTvRecommendations.indexOf(compare)) : null;
        }

        return null;
    }

    public boolean add(Recommendation rec) {
        switch (rec.getType()) {

            case Movie:
                mMovieRecommendations.add(rec);
                break;
            case Tv:
                mTvRecommendations.add(rec);
                break;
        }

        return true;
    }

    public Integer getRecId(RecommendationType type, int max) {
        switch (type) {
            case Movie:
                if (mMovieRecommendations.size() < max) return 100 + mMovieRecommendations.size()+1;
                return replaceOldest(type);
            case Tv:
                if (mTvRecommendations.size() < max) return 200 + mTvRecommendations.size()+1;
                return replaceOldest(type);
        }

        throw new IllegalArgumentException("type");
    }

    private Integer replaceOldest(RecommendationType type) {
        List<Recommendation> list = type == RecommendationType.Movie ? mMovieRecommendations : mTvRecommendations;

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

}

