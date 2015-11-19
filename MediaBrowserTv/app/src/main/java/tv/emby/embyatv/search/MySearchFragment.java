package tv.emby.embyatv.search;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
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

import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemLauncher;
import tv.emby.embyatv.imagehandling.PicassoBackgroundManagerTarget;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.util.Utils;

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
        if (!Utils.IsEmpty(newQuery)) {
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
            if (!Utils.IsEmpty(query)) {
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

        mDefaultBackground = getResources().getDrawable(R.drawable.moviebg);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }


}

