package tv.mediabrowser.mediabrowsertv.browsing;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.NextUpQuery;
import tv.mediabrowser.mediabrowsertv.TvApp;
import tv.mediabrowser.mediabrowsertv.querying.StdItemQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class BrowseViewFragment extends BrowseFolderFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    protected void setupQueries(final IRowLoader rowLoader) {
        switch (mFolder.getCollectionType().toLowerCase())
        {
            case "movies":
                itemTypeString = "Movie";

                //Resume
                StdItemQuery resumeMovies = new StdItemQuery();
                resumeMovies.setIncludeItemTypes(new String[]{"Movie"});
                resumeMovies.setRecursive(true);
                resumeMovies.setParentId(mFolder.getId());
                resumeMovies.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
                resumeMovies.setSortBy(new String[]{ItemSortBy.DatePlayed});
                resumeMovies.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef("Continue Watching", resumeMovies, 50));

                //Latest
                StdItemQuery latestMovies = new StdItemQuery();
                latestMovies.setIncludeItemTypes(new String[]{"Movie"});
                latestMovies.setRecursive(true);
                latestMovies.setParentId(mFolder.getId());
                latestMovies.setLimit(50);
                latestMovies.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
                latestMovies.setSortBy(new String[]{ItemSortBy.DateCreated});
                latestMovies.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef("Latest", latestMovies, 0));

                //All
                StdItemQuery movies = new StdItemQuery();
                movies.setIncludeItemTypes(new String[]{"Movie"});
                movies.setRecursive(true);
                movies.setParentId(mFolder.getId());
                movies.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef("By Name", movies, 100));

                //Favorites
                StdItemQuery favorites = new StdItemQuery();
                favorites.setIncludeItemTypes(new String[]{"Movie"});
                favorites.setRecursive(true);
                favorites.setParentId(mFolder.getId());
                favorites.setFilters(new ItemFilter[]{ItemFilter.IsFavorite});
                favorites.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef("Favorites", favorites, 100));

                //Collections
                StdItemQuery collections = new StdItemQuery();
                collections.setIncludeItemTypes(new String[]{"BoxSet"});
                collections.setRecursive(true);
                collections.setParentId(mFolder.getId());
                collections.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef("Collections", collections, 100));

                rowLoader.loadRows(mRows);
                break;
            case "tvshows":
                itemTypeString = "Series";

                //Next up
                NextUpQuery nextUpQuery = new NextUpQuery();
                nextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
                nextUpQuery.setLimit(50);
                nextUpQuery.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio});
                mRows.add(new BrowseRowDef("Next Up", nextUpQuery));

                //Latest content added
                StdItemQuery latestSeries = new StdItemQuery();
                latestSeries.setIncludeItemTypes(new String[]{"Series"});
                latestSeries.setRecursive(true);
                latestSeries.setParentId(mFolder.getId());
                latestSeries.setLimit(50);
                latestSeries.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
                latestSeries.setSortBy(new String[]{ItemSortBy.DateCreated});
                latestSeries.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef("Latest", latestSeries, 0));

                //All
                StdItemQuery series = new StdItemQuery();
                series.setIncludeItemTypes(new String[]{"Series"});
                series.setRecursive(true);
                series.setParentId(mFolder.getId());
                series.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef("By Name", series, 100));

                //Favorites
                StdItemQuery tvFavorites = new StdItemQuery();
                tvFavorites.setIncludeItemTypes(new String[]{"Series"});
                tvFavorites.setRecursive(true);
                tvFavorites.setParentId(mFolder.getId());
                tvFavorites.setFilters(new ItemFilter[]{ItemFilter.IsFavorite});
                tvFavorites.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef("Favorites", tvFavorites, 100));

                rowLoader.loadRows(mRows);
                break;
            default:
                // Fall back to rows defined by the view children
                final List<BrowseRowDef> rows = new ArrayList<>();
                final String userId = TvApp.getApplication().getCurrentUser().getId();

                ItemQuery query = new ItemQuery();
                query.setParentId(mFolder.getId());
                query.setUserId(userId);
                query.setSortBy(new String[]{ItemSortBy.SortName});

                TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        if (response.getTotalRecordCount() > 0) {
                            for (BaseItemDto item : response.getItems()) {
                                ItemQuery rowQuery = new ItemQuery();
                                rowQuery.setParentId(item.getId());
                                rowQuery.setUserId(userId);
                                rowQuery.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
                                rows.add(new BrowseRowDef(item.getName(), rowQuery, 100));
                            }
                        }

                        rowLoader.loadRows(rows);
                    }

                    @Override
                    public void onError(Exception exception) {
                        exception.printStackTrace();
                    }
                });
                break;
        }


    }

}
