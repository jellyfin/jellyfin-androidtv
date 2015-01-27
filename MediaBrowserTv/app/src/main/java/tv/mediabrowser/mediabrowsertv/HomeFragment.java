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
    private static final int LOGOUT = 0;
    private static final int SETTINGS = 1;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        MainTitle = this.getString(R.string.home_title);
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void setupQueries(IRowLoader rowLoader) {

        mRows.add(new BrowseRowDef("Library", new ViewQuery()));

        StdItemQuery resumeMovies = new StdItemQuery();
        resumeMovies.setIncludeItemTypes(new String[]{"Movie"});
        resumeMovies.setRecursive(true);
        resumeMovies.setLimit(50);
        resumeMovies.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
        resumeMovies.setSortBy(new String[]{ItemSortBy.DatePlayed});
        resumeMovies.setSortOrder(SortOrder.Descending);
        mRows.add(new BrowseRowDef("Continue Watching", resumeMovies, 0));

        StdItemQuery latestMovies = new StdItemQuery();
        latestMovies.setIncludeItemTypes(new String[]{"Movie"});
        latestMovies.setRecursive(true);
        latestMovies.setLimit(50);
        latestMovies.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
        latestMovies.setSortBy(new String[]{ItemSortBy.DateCreated});
        latestMovies.setSortOrder(SortOrder.Descending);
        mRows.add(new BrowseRowDef("Latest Movies", latestMovies, 0));

        NextUpQuery nextUpQuery = new NextUpQuery();
        nextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        nextUpQuery.setLimit(50);
        nextUpQuery.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio});
        mRows.add(new BrowseRowDef("Next Up TV", nextUpQuery));

//        StdItemQuery latestMusic = new StdItemQuery();
//        latestMusic.setIncludeItemTypes(new String[]{"MusicAlbum"});
//        latestMusic.setRecursive(true);
//        latestMusic.setLimit(50);
//        latestMusic.setSortBy(new String[]{ItemSortBy.DateCreated});
//        latestMusic.setSortOrder(SortOrder.Descending);
//        mRows.add(new BrowseRowDef("Latest Albums", latestMusic, 0));

        rowLoader.loadRows(mRows);
    }

    @Override
    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {
        super.addAdditionalRows(rowAdapter);
        HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), "SETTINGS", null);

        GridButtonPresenter mGridPresenter = new GridButtonPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(new GridButton(SETTINGS, "App Settings", R.drawable.gears));
        gridRowAdapter.add(new GridButton(LOGOUT, "Logout " + TvApp.getApplication().getCurrentUser().getName(), R.drawable.logout));
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

            if (item instanceof GridButton) {
                switch (((GridButton) item).getId()) {
                    case LOGOUT:
                        TvApp app = TvApp.getApplication();
                        if (app.getIsAutoLoginConfigured()) {
                            // Present user selection
                            app.setLoginApiClient(app.getApiClient());
                            Intent userIntent = new Intent(getActivity(), SelectUserActivity.class);
                            userIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            getActivity().startActivity(userIntent);

                        }

                        getActivity().finish(); //don't actually log out because we handle it ourselves

                        break;
                    case SETTINGS:
                        Intent settings = new Intent(getActivity(), SettingsActivity.class);
                        getActivity().startActivity(settings);
                        break;
                    default:
                        Toast.makeText(getActivity(), item.toString(), Toast.LENGTH_SHORT)
                                .show();
                        break;
                }
            }
        }
    }


}
