package org.jellyfin.androidtv.ui.browsing;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.querying.ItemSortBy;
import org.jellyfin.apiclient.model.querying.ItemsByNameQuery;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.koin.java.KoinJavaComponent;

public class ByGenreFragment extends BrowseFolderFragment {
    @Override
    protected void setupQueries(final IRowLoader rowLoader) {
        if (Utils.getSafeValue(mFolder.getChildCount(), 0) > 0) {
            //Get all genres for this folder
            ItemsByNameQuery genres = new ItemsByNameQuery();
            genres.setParentId(mFolder.getId().toString());
            genres.setSortBy(new String[]{ItemSortBy.SortName});
            if (includeType != null) genres.setIncludeItemTypes(new String[]{includeType});
            genres.setRecursive(true);
            genres.setUserId(TvApp.getApplication().getCurrentUser().getId());
            KoinJavaComponent.<ApiClient>get(ApiClient.class).GetGenresAsync(genres, new Response<ItemsResult>() {
                @Override
                public void onResponse(ItemsResult response) {
                    for (BaseItemDto genre : response.getItems()) {
                        StdItemQuery genreQuery = new StdItemQuery();
                        genreQuery.setParentId(mFolder.getId().toString());
                        genreQuery.setSortBy(new String[]{ItemSortBy.SortName});
                        if (includeType != null)
                            genreQuery.setIncludeItemTypes(new String[]{includeType});
                        genreQuery.setGenres(new String[]{genre.getName()});
                        genreQuery.setRecursive(true);
                        mRows.add(new BrowseRowDef(genre.getName(), genreQuery, 40));
                    }

                    if (mRows.size() < 2) setHeadersState(HEADERS_DISABLED);

                    rowLoader.loadRows(mRows);
                }
            });
        } else {
            setHeadersState(HEADERS_DISABLED);
        }
    }
}
