package org.jellyfin.androidtv.ui.livetv;

import static org.koin.java.KoinJavaComponent.inject;

import androidx.annotation.NonNull;

import org.jellyfin.androidtv.preference.SystemPreferences;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.sdk.model.api.BaseItemDto;

import kotlin.Lazy;

public class GuideFilters {
    private Lazy<SystemPreferences> systemPreferences = inject(SystemPreferences.class);

    private boolean movies;
    private boolean news;
    private boolean series;
    private boolean kids;
    private boolean sports;
    private boolean premiere;

    public GuideFilters() {
        load();
    }

    public void load() {
        movies = systemPreferences.getValue().get(SystemPreferences.Companion.getLiveTvGuideFilterMovies());
        news = systemPreferences.getValue().get(SystemPreferences.Companion.getLiveTvGuideFilterNews());
        series = systemPreferences.getValue().get(SystemPreferences.Companion.getLiveTvGuideFilterSeries());
        kids = systemPreferences.getValue().get(SystemPreferences.Companion.getLiveTvGuideFilterKids());
        sports = systemPreferences.getValue().get(SystemPreferences.Companion.getLiveTvGuideFilterSports());
        premiere = systemPreferences.getValue().get(SystemPreferences.Companion.getLiveTvGuideFilterPremiere());
    }

    public boolean any() { return movies || news || series || kids || sports || premiere; }

    public boolean passesFilter(BaseItemDto program) {
        if (!any()) return true;

        if (movies && Utils.isTrue(program.isMovie())) return !premiere || Utils.isTrue(program.isPremiere());
        if (news && Utils.isTrue(program.isNews())) return !premiere || Utils.isTrue(program.isPremiere()) || Utils.isTrue(program.isLive()) || !Utils.isTrue(program.isRepeat());
        if (series && Utils.isTrue(program.isSeries())) return !premiere || Utils.isTrue(program.isPremiere()) || !Utils.isTrue(program.isRepeat());
        if (kids && Utils.isTrue(program.isKids())) return !premiere || Utils.isTrue(program.isPremiere());
        if (sports && Utils.isTrue(program.isSports())) return !premiere || Utils.isTrue(program.isPremiere()) || Utils.isTrue(program.isLive());
        if (!movies && !news && !series && !kids && !sports) return (premiere && (Utils.isTrue(program.isPremiere()) || (Utils.isTrue(program.isSeries()) && !Utils.isTrue(program.isRepeat())) || (Utils.isTrue(program.isSports()) && Utils.isTrue(program.isLive()))));

        return false;

    }

    public void clear() {
        setNews(false);
        setSeries(false);
        setSports(false);
        setKids(false);
        setMovies(false);
        setPremiere(false);
    }

    @NonNull
    @Override
    public String toString() {
        return any() ? "Content filtered. Showing channels with " + getFilterString() : "Showing all programs ";
    }

    private String getFilterString() {
        String filterString = "";
        if (movies) filterString += "movies";
        if (news) filterString += getSeparator(filterString) + "news";
        if (sports) filterString += getSeparator(filterString) + "sports";
        if (series) filterString += getSeparator(filterString) + "series";
        if (kids) filterString += getSeparator(filterString) + "kids";
        if (premiere) filterString += getSeparator(filterString) + "ONLY new";

        return filterString;
    }

    private String getSeparator(String original) {return !original.isEmpty() ? ", " : "";}

    public boolean isMovies() {
        return movies;
    }

    public void setMovies(boolean movies) {
        this.movies = movies;
        systemPreferences.getValue().set(SystemPreferences.Companion.getLiveTvGuideFilterMovies(), movies);
    }

    public boolean isNews() {
        return news;
    }

    public void setNews(boolean news) {
        this.news = news;
        systemPreferences.getValue().set(SystemPreferences.Companion.getLiveTvGuideFilterNews(), news);
    }

    public boolean isSeries() {
        return series;
    }

    public void setSeries(boolean series) {
        this.series = series;
        systemPreferences.getValue().set(SystemPreferences.Companion.getLiveTvGuideFilterSeries(), series);
    }

    public boolean isKids() {
        return kids;
    }

    public void setKids(boolean kids) {
        this.kids = kids;
        systemPreferences.getValue().set(SystemPreferences.Companion.getLiveTvGuideFilterKids(), kids);
    }

    public boolean isSports() {
        return sports;
    }

    public void setSports(boolean sports) {
        this.sports = sports;
        systemPreferences.getValue().set(SystemPreferences.Companion.getLiveTvGuideFilterSports(), sports);
    }

    public boolean isPremiere() {
        return premiere;
    }

    public void setPremiere(boolean premiere) {
        this.premiere = premiere;
        systemPreferences.getValue().set(SystemPreferences.Companion.getLiveTvGuideFilterPremiere(), premiere);
    }
}
