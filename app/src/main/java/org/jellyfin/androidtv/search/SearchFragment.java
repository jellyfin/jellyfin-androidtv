package org.jellyfin.androidtv.search;

import android.os.Bundle;
import android.os.Handler;

import org.jellyfin.androidtv.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.util.Utils;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

/**
 * Created by Eric on 1/26/2015.
 */
public class SearchFragment extends SearchSupportFragment
        implements SearchSupportFragment.SearchResultProvider {

    private static final int SEARCH_DELAY_MS = 1500;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private SearchRunnable mDelayedLoad;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);
        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (!(item instanceof BaseRowItem)) return;
                ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow) row).getAdapter(), ((BaseRowItem) item).getIndex(), getActivity());
            }
        });
        mDelayedLoad = new SearchRunnable(getActivity(), mRowsAdapter, getActivity().getIntent().getBooleanExtra("MusicOnly", false));
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
     * @param query String to search for
     * @param delayed When true the search is delayed by [SEARCH_DELAY_MS] milliseconds
     */
    private void search(String query, boolean delayed) {
        // Clear results when query is empty
        if (Utils.isEmpty(query)) {
            mRowsAdapter.clear();
            return;
        }

        // Remove current delayed search (if any)
        mHandler.removeCallbacks(mDelayedLoad);

        // Update search string
        mDelayedLoad.setQueryString(query);

        // Schedule search depending on [delayed]
        if (delayed) mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        else mHandler.post(mDelayedLoad);
    }
}

