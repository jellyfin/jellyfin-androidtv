package tv.emby.embyatv.browsing;

import android.os.Bundle;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.SimilarItemsQuery;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.querying.QueryType;
import tv.emby.embyatv.querying.StdItemQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class SuggestedMoviesFragment extends EnhancedBrowseFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        showViews = false;
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    protected void setupQueries(final IRowLoader rowLoader) {

        StdItemQuery lastPlayed = new StdItemQuery();
        lastPlayed.setParentId(mFolder.getId());
        lastPlayed.setIncludeItemTypes(new String[]{"Movie"});
        lastPlayed.setUserId(TvApp.getApplication().getCurrentUser().getId());
        lastPlayed.setSortOrder(SortOrder.Descending);
        lastPlayed.setSortBy(new String[]{ItemSortBy.DatePlayed});
        lastPlayed.setLimit(8);
        lastPlayed.setRecursive(true);

        TvApp.getApplication().getApiClient().GetItemsAsync(lastPlayed, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                for (BaseItemDto item : response.getItems()) {
                    SimilarItemsQuery similar = new SimilarItemsQuery();
                    similar.setId(item.getId());
                    similar.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.Overview});
                    similar.setLimit(7);
                    mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_because_you_watched)+item.getName(), similar, QueryType.SimilarMovies));
                }

                rowLoader.loadRows(mRows);
            }
        });

    }


}
