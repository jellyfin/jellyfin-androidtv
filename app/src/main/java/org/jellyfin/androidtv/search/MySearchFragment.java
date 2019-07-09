package org.jellyfin.androidtv.search;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.SearchFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;

import com.squareup.picasso.Target;

import org.jellyfin.androidtv.imagehandling.PicassoBackgroundManagerTarget;
import org.jellyfin.androidtv.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.util.Utils;

import java.util.Timer;

/**
 * Created by Eric on 1/26/2015.
 */
public class MySearchFragment extends SearchFragment
        implements SearchFragment.SearchResultProvider {

    private static final int SEARCH_DELAY_MS = 1500;
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
                ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow)row).getAdapter(),((BaseRowItem) item).getIndex(), getActivity());
            }
        });
        mDelayedLoad = new SearchRunnable(getActivity(), mRowsAdapter, getActivity().getIntent().getBooleanExtra("MusicOnly",false));

        prepareBackgroundManager();
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    private boolean textEdited = false;

    @Override
    public boolean onQueryTextChange(String newQuery) {
        textEdited = true;
        if (Utils.isNonEmpty(newQuery)) {
            mHandler.removeCallbacks(mDelayedLoad);
            mDelayedLoad.setQueryString(newQuery);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        } else {
            mRowsAdapter.clear();
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (!textEdited) {
            if (Utils.isNonEmpty(query)) {
                mHandler.removeCallbacks(mDelayedLoad);
                mDelayedLoad.setQueryString(query);
                mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
            } else {
                mRowsAdapter.clear();
            }
        }
        return true;
    }

    private void prepareBackgroundManager() {

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = null;

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }


}

