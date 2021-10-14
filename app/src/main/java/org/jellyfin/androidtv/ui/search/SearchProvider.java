package org.jellyfin.androidtv.ui.search;

import android.content.Context;
import android.os.Handler;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ObjectAdapter;

import org.jellyfin.androidtv.ui.presentation.CustomListRowPresenter;
import org.jellyfin.androidtv.util.Utils;

public class SearchProvider implements SearchSupportFragment.SearchResultProvider {
    private static final int SEARCH_DELAY_MS = 600;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private SearchRunnable mDelayedLoad;
    private String lastQuery;

    SearchProvider(Context context, boolean musicOnly) {
        mRowsAdapter = new ArrayObjectAdapter(new CustomListRowPresenter());
        mDelayedLoad = new SearchRunnable(context, mRowsAdapter, musicOnly);
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        search(query, true);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        search(query, false);

        return true;
    }

    /**
     * Update search results
     *
     * @param query   String to search for
     * @param delayed When true the search is delayed by [SEARCH_DELAY_MS] milliseconds
     */
    private void search(String query, boolean delayed) {
        // Clear results when query is empty
        if (Utils.isEmpty(query)) {
            mRowsAdapter.clear();
            return;
        }

        // Don't search the same thing twice
        if (query.equals(lastQuery)) return;
        lastQuery = query;

        // Remove current delayed search (if any)
        mHandler.removeCallbacks(mDelayedLoad);

        // Update search string
        mDelayedLoad.setQueryString(query);

        // Schedule search depending on [delayed]
        if (delayed) mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        else mHandler.post(mDelayedLoad);
    }
}
