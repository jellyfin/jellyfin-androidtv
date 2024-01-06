package org.jellyfin.androidtv.ui.browsing;

import android.os.Bundle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.data.querying.SpecialsQuery;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.entities.SortOrder;
import org.jellyfin.apiclient.model.querying.ItemFilter;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.constant.ItemSortBy;

import java.util.Arrays;

public class GenericFolderFragment extends EnhancedBrowseFragment {
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private static BaseItemKind[] showSpecialViewTypes = new BaseItemKind[]{
            BaseItemKind.COLLECTION_FOLDER,
            BaseItemKind.FOLDER,
            BaseItemKind.USER_VIEW,
            BaseItemKind.CHANNEL_FOLDER_ITEM
    };

    @Override
    protected void setupQueries(RowLoader rowLoader) {
        if (Utils.getSafeValue(mFolder.getChildCount(), 0) > 0 ||
                mFolder.getType() == BaseItemKind.CHANNEL ||
                mFolder.getType() == BaseItemKind.CHANNEL_FOLDER_ITEM ||
                mFolder.getType() == BaseItemKind.USER_VIEW ||
                mFolder.getType() == BaseItemKind.COLLECTION_FOLDER) {
            boolean showSpecialViews = Arrays.asList(showSpecialViewTypes).contains(mFolder.getType());

            if (showSpecialViews) {
                if (mFolder.getType() != BaseItemKind.CHANNEL_FOLDER_ITEM) {
                    StdItemQuery resume = new StdItemQuery();
                    resume.setParentId(mFolder.getId().toString());
                    resume.setLimit(50);
                    resume.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
                    resume.setSortBy(new String[]{ItemSortBy.DatePlayed});
                    resume.setSortOrder(SortOrder.Descending);
                    mRows.add(new BrowseRowDef(getString(R.string.lbl_continue_watching), resume, 0));
                }

                StdItemQuery latest = new StdItemQuery();
                latest.setParentId(mFolder.getId().toString());
                latest.setLimit(50);
                latest.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
                latest.setSortBy(new String[]{ItemSortBy.DateCreated});
                latest.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(getString(R.string.lbl_latest), latest, 0));

            }

            StdItemQuery byName = new StdItemQuery();
            byName.setParentId(mFolder.getId().toString());
            String header = (mFolder.getType() == BaseItemKind.SEASON) ? mFolder.getName() : getString(R.string.lbl_by_name);
            mRows.add(new BrowseRowDef(header, byName, 100));

            if (mFolder.getType() == BaseItemKind.SEASON) {
                SpecialsQuery specials = new SpecialsQuery(mFolder.getId().toString());
                mRows.add(new BrowseRowDef(getString(R.string.lbl_specials), specials));
            }

            rowLoader.loadRows(mRows);
        }
    }
}
