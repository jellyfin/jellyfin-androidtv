package tv.mediabrowser.mediabrowsertv.search;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;

import com.squareup.picasso.Target;

import java.util.Timer;

import mediabrowser.model.search.SearchQuery;
import tv.mediabrowser.mediabrowsertv.itemhandling.BaseRowItem;
import tv.mediabrowser.mediabrowsertv.itemhandling.ItemLauncher;
import tv.mediabrowser.mediabrowsertv.itemhandling.ItemRowAdapter;
import tv.mediabrowser.mediabrowsertv.imagehandling.PicassoBackgroundManagerTarget;
import tv.mediabrowser.mediabrowsertv.R;
import tv.mediabrowser.mediabrowsertv.TvApp;
import tv.mediabrowser.mediabrowsertv.presentation.CardPresenter;
import tv.mediabrowser.mediabrowsertv.util.Utils;

/**
 * Created by Eric on 1/26/2015.
 */
public class MySearchFragment extends SearchFragment
        implements SearchFragment.SearchResultProvider {

    private static final int SEARCH_DELAY_MS = 800;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private SearchRunnable mDelayedLoad;

    private Drawable mDefaultBackground;
    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private String mBackgroundUrl;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);
        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (!(item instanceof BaseRowItem)) return;
                ItemLauncher.launch((BaseRowItem) item, TvApp.getApplication(), getActivity(), itemViewHolder);
            }
        });
        mDelayedLoad = new SearchRunnable();

        prepareBackgroundManager();
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        mRowsAdapter.clear();
        if (!Utils.IsEmpty(newQuery)) {
            mHandler.removeCallbacks(mDelayedLoad);
            mDelayedLoad.setQueryString(newQuery);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mRowsAdapter.clear();
        if (!Utils.IsEmpty(query)) {
            mHandler.removeCallbacks(mDelayedLoad);
            mDelayedLoad.setQueryString(query);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }
        return true;
    }

    private void prepareBackgroundManager() {

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(R.drawable.moviebg);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private class SearchRunnable implements Runnable {
        private String searchString;

        public void setQueryString(String value) {
            searchString = value;
        }

        @Override
        public void run() {
            //Get search results by type
            SearchQuery people = getSearchQuery(new String[] {"Person"});
            ItemRowAdapter peopleAdapter = new ItemRowAdapter(people, new CardPresenter(), mRowsAdapter);
            ListRow peopleRow = new ListRow(new HeaderItem(getActivity().getString(R.string.lbl_people),""), peopleAdapter);
            peopleAdapter.setRow(peopleRow);
            peopleAdapter.Retrieve();

            SearchQuery tv = getSearchQuery(new String[] {"Series","Episode"});
            ItemRowAdapter tvAdapter = new ItemRowAdapter(tv, new CardPresenter(), mRowsAdapter);
            ListRow tvRow = new ListRow(new HeaderItem(getActivity().getString(R.string.lbl_tv),""), tvAdapter);
            tvAdapter.setRow(tvRow);
            tvAdapter.Retrieve();

            SearchQuery movies = getSearchQuery(new String[] {"Movie", "BoxSet"});
            ItemRowAdapter movieAdapter = new ItemRowAdapter(movies, new CardPresenter(), mRowsAdapter);
            ListRow movieRow = new ListRow(new HeaderItem(getActivity().getString(R.string.lbl_movies),""), movieAdapter);
            movieAdapter.setRow(movieRow);
            movieAdapter.Retrieve();

        }

        private SearchQuery getSearchQuery(String[] itemTypes) {
            SearchQuery query = new SearchQuery();
            query.setLimit(50);
            query.setSearchTerm(searchString);
            query.setIncludeItemTypes(itemTypes);

            return query;

        }
    }

}

