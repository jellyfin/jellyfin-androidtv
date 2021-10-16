package org.jellyfin.androidtv.ui.search;

import android.content.Context;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.model.search.SearchQuery;

import java.util.ArrayList;

import timber.log.Timber;

public class SearchRunnable implements Runnable {
    private String searchString;
    private Context context;
    private ArrayObjectAdapter mRowsAdapter;
    private int searchesReceived;
    private ArrayList<ItemRowAdapter> searchItemRows;

    public void setQueryString(String value) {
        searchString = value;
    }

    public SearchRunnable(Context context, ArrayObjectAdapter adapter) {
        this.context = context;
        this.mRowsAdapter = adapter;
        this.searchItemRows = new ArrayList<>();
    }

    @Override
    public void run() {
        mRowsAdapter.clear();
        searchItemRows.clear();
        searchesReceived = 0;

        //Get search results by type
        SearchQuery movies = getSearchQuery(new String[]{"Movie", "BoxSet"});
        ItemRowAdapter movieAdapter = new ItemRowAdapter(movies, new CardPresenter(), mRowsAdapter);
        ListRow movieRow = new ListRow(new HeaderItem(context.getString(R.string.lbl_movies)), movieAdapter);
        movieAdapter.setRow(movieRow);
        retrieveSearchResult(movieAdapter);

        SearchQuery tvSeries = getSearchQuery(new String[]{"Series"});
        ItemRowAdapter tvSeriesAdapter = new ItemRowAdapter(tvSeries, new CardPresenter(), mRowsAdapter);
        ListRow tvSeriesRow = new ListRow(new HeaderItem(context.getString(R.string.lbl_series)), tvSeriesAdapter);
        tvSeriesAdapter.setRow(tvSeriesRow);
        retrieveSearchResult(tvSeriesAdapter);

        SearchQuery tv = getSearchQuery(new String[]{"Episode"});
        ItemRowAdapter tvAdapter = new ItemRowAdapter(tv, new CardPresenter(), mRowsAdapter);
        ListRow tvRow = new ListRow(new HeaderItem(context.getString(R.string.lbl_episodes)), tvAdapter);
        tvAdapter.setRow(tvRow);
        retrieveSearchResult(tvAdapter);

        SearchQuery people = getSearchQuery(new String[]{"Person", "People"});
        ItemRowAdapter peopleAdapter = new ItemRowAdapter(people, new CardPresenter(), mRowsAdapter);
        ListRow peopleRow = new ListRow(new HeaderItem(context.getString(R.string.lbl_people)), peopleAdapter);
        peopleAdapter.setRow(peopleRow);
        retrieveSearchResult(peopleAdapter);

        SearchQuery videos = getSearchQuery(new String[]{"Video"});
        ItemRowAdapter videoAdapter = new ItemRowAdapter(videos, new CardPresenter(), mRowsAdapter);
        ListRow videoRow = new ListRow(new HeaderItem(context.getString(R.string.lbl_videos)), videoAdapter);
        videoAdapter.setRow(videoRow);
        retrieveSearchResult(videoAdapter);

        SearchQuery recordings = getSearchQuery(new String[]{"Recording"});
        ItemRowAdapter recordingAdapter = new ItemRowAdapter(recordings, new CardPresenter(), mRowsAdapter);
        ListRow recordingRow = new ListRow(new HeaderItem(context.getString(R.string.lbl_recordings)), recordingAdapter);
        recordingAdapter.setRow(recordingRow);
        retrieveSearchResult(recordingAdapter);

        SearchQuery programs = getSearchQuery(new String[]{"Program"});
        ItemRowAdapter programAdapter = new ItemRowAdapter(programs, new CardPresenter(), mRowsAdapter);
        ListRow programRow = new ListRow(new HeaderItem(context.getString(R.string.lbl_programs)), programAdapter);
        programAdapter.setRow(programRow);
        retrieveSearchResult(programAdapter);

        SearchQuery artists = getSearchQuery(new String[]{"MusicArtist"});
        ItemRowAdapter artistAdapter = new ItemRowAdapter(artists, new CardPresenter(), mRowsAdapter);
        ListRow artistRow = new ListRow(new HeaderItem(context.getString(R.string.lbl_artists)), artistAdapter);
        artistAdapter.setRow(artistRow);
        retrieveSearchResult(artistAdapter);

        SearchQuery albums = getSearchQuery(new String[]{"MusicAlbum"});
        ItemRowAdapter albumAdapter = new ItemRowAdapter(albums, new CardPresenter(), mRowsAdapter);
        ListRow albumRow = new ListRow(new HeaderItem(context.getString(R.string.lbl_albums)), albumAdapter);
        albumAdapter.setRow(albumRow);
        retrieveSearchResult(albumAdapter);

        SearchQuery songs = getSearchQuery(new String[]{"Audio"});
        ItemRowAdapter songAdapter = new ItemRowAdapter(songs, new CardPresenter(), mRowsAdapter);
        ListRow songRow = new ListRow(new HeaderItem(context.getString(R.string.lbl_songs)), songAdapter);
        songAdapter.setRow(songRow);
        retrieveSearchResult(songAdapter);
    }

    private void retrieveSearchResult(ItemRowAdapter itemRow) {
        searchItemRows.add(itemRow);

        itemRow.setRetrieveFinishedListener(new EmptyResponse() {
            @Override
            public void onResponse() {
                searchesReceived++;
                if (searchesReceived == searchItemRows.size()) {
                    for (ItemRowAdapter itemRowAdapter : searchItemRows) {
                        itemRowAdapter.addToParentIfResultsReceived();
                    }
                }
            }

            @Override
            public void onError(Exception ex) {
                // Log the error but call onResponse to make sure items will be displayed
                // even if one of the requests failed
                Timber.e(ex);
                onResponse();
            }
        });

        itemRow.Retrieve();
    }

    private SearchQuery getSearchQuery(String[] itemTypes) {
        SearchQuery query = new SearchQuery();
        query.setLimit(50);
        query.setSearchTerm(searchString);
        query.setIncludeItemTypes(itemTypes);

        return query;
    }
}

