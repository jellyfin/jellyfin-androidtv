package tv.mediabrowser.mediabrowsertv;

import android.os.Bundle;

import java.util.Arrays;

import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;

/**
 * Created by Eric on 12/4/2014.
 */
public class CollectionFragment extends BrowseFolderFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void setupQueries(IRowLoader rowLoader) {

        StdItemQuery byDate = new StdItemQuery();
        byDate.setParentId(mFolder.getId());
        byDate.setSortBy(new String[]{ItemSortBy.PremiereDate});
        mRows.add(new BrowseRowDef("By Release", byDate, 100));

        setHeadersState(HEADERS_DISABLED);

        rowLoader.loadRows(mRows);

    }


}
