package org.jellyfin.androidtv.ui.browsing;

import android.os.Bundle;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.util.DelayedMessage;

import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.querying.ItemSortBy;
import org.jellyfin.apiclient.model.querying.ItemsByNameQuery;
import org.jellyfin.apiclient.model.querying.ItemsResult;

public class ByGenreFragment extends CustomViewFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    protected void setupQueries(final IRowLoader rowLoader) {

        if (Utils.getSafeValue(mFolder.getChildCount(), 0) > 0) {
            final DelayedMessage message = new DelayedMessage(getActivity());

            //Get all genres for this folder
            ItemsByNameQuery genres = new ItemsByNameQuery();
            genres.setParentId(mFolder.getId());
            genres.setSortBy(new String[]{ItemSortBy.SortName});
            if (includeType != null) genres.setIncludeItemTypes(new String[]{includeType});
            genres.setRecursive(true);
            genres.setUserId(TvApp.getApplication().getCurrentUser().getId());
            TvApp.getApplication().getApiClient().GetGenresAsync(genres, new Response<ItemsResult>() {
                @Override
                public void onResponse(ItemsResult response) {
                    for (BaseItemDto genre : response.getItems()) {
                        StdItemQuery genreQuery = new StdItemQuery();
                        genreQuery.setParentId(mFolder.getId());
                        genreQuery.setSortBy(new String[]{ItemSortBy.SortName});
                        if (includeType != null) genreQuery.setIncludeItemTypes(new String[]{includeType});
                        genreQuery.setGenres(new String[] {genre.getName()});
                        genreQuery.setRecursive(true);
                        mRows.add(new BrowseRowDef(genre.getName(), genreQuery, 40));
                    }

                    if (mRows.size() < 2) setHeadersState(HEADERS_DISABLED);

                    rowLoader.loadRows(mRows);

                    message.Cancel();
                }
            });
        }
        else {
            setHeadersState(HEADERS_DISABLED);
        }
    }


}
