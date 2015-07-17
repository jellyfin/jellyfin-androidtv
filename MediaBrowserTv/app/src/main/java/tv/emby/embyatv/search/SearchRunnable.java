package tv.emby.embyatv.search;

import android.app.Activity;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;

import mediabrowser.model.search.SearchQuery;
import tv.emby.embyatv.R;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.presentation.CardPresenter;

/**
 * Created by Eric on 4/8/2015.
 */
public class SearchRunnable implements Runnable {
    private String searchString;
    private Activity activity;
    private ArrayObjectAdapter mRowsAdapter;

    public void setQueryString(String value) {
        searchString = value;
    }

    public SearchRunnable(Activity activity, ArrayObjectAdapter adapter) {
        this.activity = activity;
        this.mRowsAdapter = adapter;
    }

    @Override
    public void run() {
        mRowsAdapter.clear();

        //Get search results by type
        SearchQuery people = getSearchQuery(new String[] {"Person"});
        ItemRowAdapter peopleAdapter = new ItemRowAdapter(people, new CardPresenter(), mRowsAdapter);
        ListRow peopleRow = new ListRow(new HeaderItem(activity.getString(R.string.lbl_people),""), peopleAdapter);
        peopleAdapter.setRow(peopleRow);
        peopleAdapter.Retrieve();

        SearchQuery movies = getSearchQuery(new String[] {"Movie", "BoxSet"});
        ItemRowAdapter movieAdapter = new ItemRowAdapter(movies, new CardPresenter(), mRowsAdapter);
        ListRow movieRow = new ListRow(new HeaderItem(activity.getString(R.string.lbl_movies),""), movieAdapter);
        movieAdapter.setRow(movieRow);
        movieAdapter.Retrieve();

        SearchQuery tv = getSearchQuery(new String[] {"Series","Episode"});
        ItemRowAdapter tvAdapter = new ItemRowAdapter(tv, new CardPresenter(), mRowsAdapter);
        ListRow tvRow = new ListRow(new HeaderItem(activity.getString(R.string.lbl_tv),""), tvAdapter);
        tvAdapter.setRow(tvRow);
        tvAdapter.Retrieve();

        SearchQuery videos = getSearchQuery(new String[] {"Video"});
        ItemRowAdapter videoAdapter = new ItemRowAdapter(videos, new CardPresenter(), mRowsAdapter);
        ListRow videoRow = new ListRow(new HeaderItem(activity.getString(R.string.lbl_videos),""), videoAdapter);
        videoAdapter.setRow(videoRow);
        videoAdapter.Retrieve();

        SearchQuery recordings = getSearchQuery(new String[] {"Recording"});
        ItemRowAdapter recordingAdapter = new ItemRowAdapter(recordings, new CardPresenter(), mRowsAdapter);
        ListRow recordingRow = new ListRow(new HeaderItem(activity.getString(R.string.lbl_recordings),""), recordingAdapter);
        recordingAdapter.setRow(recordingRow);
        recordingAdapter.Retrieve();

    }

    private SearchQuery getSearchQuery(String[] itemTypes) {
        SearchQuery query = new SearchQuery();
        query.setLimit(50);
        query.setSearchTerm(searchString);
        query.setIncludeItemTypes(itemTypes);

        return query;

    }
}

