package tv.mediabrowser.mediabrowsertv;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.NextUpQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class HomeFragment extends StdBrowseFragment {
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        MainTitle = this.getString(R.string.home_title);
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void setupQueries() {

        mRows.add(new BrowseRowDef("Library", new ViewQuery()));

        StdItemQuery resumeMovies = new StdItemQuery();
        resumeMovies.setIncludeItemTypes(new String[]{"Movie"});
        resumeMovies.setRecursive(true);
        resumeMovies.setLimit(50);
        resumeMovies.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
        resumeMovies.setSortBy(new String[]{ItemSortBy.DatePlayed});
        resumeMovies.setSortOrder(SortOrder.Descending);
        mRows.add(new BrowseRowDef("Continue Watching", resumeMovies));

        StdItemQuery latestMovies = new StdItemQuery();
        latestMovies.setIncludeItemTypes(new String[]{"Movie"});
        latestMovies.setRecursive(true);
        latestMovies.setLimit(50);
        latestMovies.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
        latestMovies.setSortBy(new String[]{ItemSortBy.DateCreated});
        latestMovies.setSortOrder(SortOrder.Descending);
        mRows.add(new BrowseRowDef("Latest Movies", latestMovies));

        NextUpQuery nextUpQuery = new NextUpQuery();
        nextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        nextUpQuery.setLimit(50);
        nextUpQuery.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio});
        mRows.add(new BrowseRowDef("Next Up TV", nextUpQuery));

        StdItemQuery latestMusic = new StdItemQuery();
        latestMusic.setIncludeItemTypes(new String[]{"MusicAlbum"});
        latestMusic.setRecursive(true);
        latestMusic.setLimit(50);
        latestMusic.setSortBy(new String[]{ItemSortBy.DateCreated});
        latestMusic.setSortOrder(SortOrder.Descending);
        mRows.add(new BrowseRowDef("Latest Albums", latestMusic));
    }

    @Override
    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {
        super.addAdditionalRows(rowAdapter);
        HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), "SETTINGS", null);

        GridItemPresenter mGridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(getResources().getString(R.string.grid_view));
        gridRowAdapter.add(getResources().getString(R.string.error_fragment));
        gridRowAdapter.add(getResources().getString(R.string.personal_settings));
        gridRowAdapter.add("Logout " + TvApp.getApplication().getCurrentUser().getName());
        rowAdapter.add(new ListRow(gridHeader, gridRowAdapter));
    }

    @Override
    protected void setupEventListeners() {
        super.setupEventListeners();
        mClickedListener.registerListener(new ItemViewClickedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof String) {
                if (((String) item).indexOf(getString(R.string.error_fragment)) >= 0) {
                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
                    intent.putExtra("Message", "Test Error");
                    startActivity(intent);
                } else if (((String) item).indexOf("Logout ") >= 0) {
                    mApiClient = TvApp.getApplication().getConnectionManager().GetApiClient(TvApp.getApplication().getCurrentUser());
                    mApiClient.Logout(new EmptyResponse() {
                        @Override
                        public void onResponse() {
                            super.onResponse();
                            Intent intent = new Intent(getActivity(), StartupActivity.class);
                            startActivity(intent);
                        }
                    });
                } else
                {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(getResources().getColor(R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

}
