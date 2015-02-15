package tv.mediabrowser.mediabrowsertv.browsing;

import android.os.Bundle;

import java.util.Arrays;

import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;
import tv.mediabrowser.mediabrowsertv.querying.StdItemQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class GenericFolderFragment extends BrowseFolderFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    private static String[] showSpecialViewTypes = new String[] {"CollectionFolder", "Folder", "Channel", "ChannelFolderItem"};

    @Override
    protected void setupQueries(IRowLoader rowLoader) {

        if (mFolder.getChildCount() > 0) {
            boolean showSpecialViews = Arrays.asList(showSpecialViewTypes).contains(mFolder.getType());

            if (showSpecialViews) {
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

            }


            StdItemQuery byName = new StdItemQuery();
            byName.setParentId(mFolder.getId());
            byName.setSortBy(new String[]{ItemSortBy.SortName});
            mRows.add(new BrowseRowDef("By Name", byName, 100));

            if (mRows.size() < 2) setHeadersState(HEADERS_DISABLED);

            rowLoader.loadRows(mRows);

        }
        else {
            setHeadersState(HEADERS_DISABLED);
        }
    }


}
