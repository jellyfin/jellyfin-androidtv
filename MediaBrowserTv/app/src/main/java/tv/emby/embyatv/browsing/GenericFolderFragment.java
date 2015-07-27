package tv.emby.embyatv.browsing;

import android.os.Bundle;

import java.util.Arrays;

import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;
import tv.emby.embyatv.R;
import tv.emby.embyatv.querying.StdItemQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class GenericFolderFragment extends EnhancedBrowseFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    private static String[] showSpecialViewTypes = new String[] {"CollectionFolder", "Folder", "UserView", "ChannelFolderItem"};

    @Override
    protected void setupQueries(IRowLoader rowLoader) {

        if (mFolder.getChildCount() > 0 || mFolder.getType().equals("Channel") || mFolder.getType().equals("ChannelFolderItem") || mFolder.getType().equals("UserView")) {
            boolean showSpecialViews = Arrays.asList(showSpecialViewTypes).contains(mFolder.getType()) && !"channels".equals(mFolder.getCollectionType());

            if (showSpecialViews) {
                if (!mFolder.getType().equals("ChannelFolderItem")) {
                    StdItemQuery resume = new StdItemQuery();
                    resume.setParentId(mFolder.getId());
                    resume.setLimit(50);
                    resume.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
                    resume.setSortBy(new String[]{ItemSortBy.DatePlayed});
                    resume.setSortOrder(SortOrder.Descending);
                    mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_continue_watching), resume, 0));
                }

                StdItemQuery latest = new StdItemQuery();
                latest.setParentId(mFolder.getId());
                latest.setLimit(50);
                latest.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
                latest.setSortBy(new String[]{ItemSortBy.DateCreated});
                latest.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest_additions), latest, 0));

            }


            StdItemQuery byName = new StdItemQuery();
            byName.setParentId(mFolder.getId());
            byName.setSortBy(new String[]{ItemSortBy.SortName});
            mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_by_name), byName, 100));

            rowLoader.loadRows(mRows);

        }
    }


}
