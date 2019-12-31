package org.jellyfin.androidtv.search;

import android.os.Bundle;
import android.os.Handler;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.util.Utils;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.SpeechOrbView;

/**
 * Created by Eric on 1/26/2015.
 */
public class SearchFragment extends SearchSupportFragment
        implements SearchSupportFragment.SearchResultProvider {

    private static final int SEARCH_DELAY_MS = 1500;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private SearchRunnable mDelayedLoad;
    private boolean isSpeechEnabled = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isSpeechEnabled = SpeechRecognizer.isRecognitionAvailable(getContext());
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);
        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if (!(item instanceof BaseRowItem)) return;

            ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow) row).getAdapter(), ((BaseRowItem) item).getIndex(), getActivity());
        });
        mDelayedLoad = new SearchRunnable(getActivity(), mRowsAdapter, getActivity().getIntent().getBooleanExtra("MusicOnly", false));

        // Disable speech functionality
        if (!isSpeechEnabled) {
            // This function is deprecated but it still disabled the automatic speech functionality
            // when a callback is set. We don't actually implement it.
            setSpeechRecognitionCallback(() -> {
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) return null;

        // Hide speech orb when speech is disabled
        if (!isSpeechEnabled) {
            // Set to invisible instead of gone to keep the alignment of the input
            SpeechOrbView speechOrbView = view.findViewById(R.id.lb_search_bar_speech_orb);
            speechOrbView.setVisibility(View.INVISIBLE);
        }

        return view;
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

        // Remove current delayed search (if any)
        mHandler.removeCallbacks(mDelayedLoad);

        // Update search string
        mDelayedLoad.setQueryString(query);

        // Schedule search depending on [delayed]
        if (delayed) mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        else mHandler.post(mDelayedLoad);
    }
}

