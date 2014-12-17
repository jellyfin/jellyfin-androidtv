package tv.mediabrowser.mediabrowsertv;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.ItemsResult;

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
        // Our rows are defined by the view children
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

    }
}
