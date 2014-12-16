package tv.mediabrowser.mediabrowsertv;

import android.os.Bundle;

import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.NextUpQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class GenericFolderFragment extends BrowseFolderFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        ShowBadge = false;
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void setupQueries(IRowLoader rowLoader) {

        StdItemQuery resume = new StdItemQuery();
        resume.setParentId(mFolder.getId());
        resume.setLimit(50);
        resume.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
        resume.setSortBy(new String[]{ItemSortBy.DatePlayed});
        resume.setSortOrder(SortOrder.Descending);
        mRows.add(new BrowseRowDef("Continue Watching", resume, 0));

        StdItemQuery latest = new StdItemQuery();
        latest.setParentId(mFolder.getId());
        latest.setLimit(50);
        latest.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
        latest.setSortBy(new String[]{ItemSortBy.DateCreated});
        latest.setSortOrder(SortOrder.Descending);
        mRows.add(new BrowseRowDef("Latest Additions", latest, 0));

        StdItemQuery byName = new StdItemQuery();
        byName.setParentId(mFolder.getId());
        byName.setLimit(100);
        byName.setSortBy(new String[]{ItemSortBy.SortName});
        mRows.add(new BrowseRowDef("By Name", byName, 100));

        rowLoader.loadRows(mRows);

    }


}
