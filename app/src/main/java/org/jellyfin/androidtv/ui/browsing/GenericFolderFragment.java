package org.jellyfin.androidtv.ui.browsing;

import android.os.Bundle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.model.entities.SortOrder;
import org.jellyfin.apiclient.model.querying.ItemFilter;
import org.jellyfin.sdk.model.api.BaseItemDto;
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
        BaseItemDto folder = ModelCompat.asSdk(mFolder);

        if (Utils.getSafeValue(folder.getChildCount(), 0) > 0 ||
                folder.getType() == BaseItemKind.CHANNEL ||
                folder.getType() == BaseItemKind.CHANNEL_FOLDER_ITEM ||
                folder.getType() == BaseItemKind.USER_VIEW ||
                folder.getType() == BaseItemKind.COLLECTION_FOLDER) {
            boolean showSpecialViews = Arrays.asList(showSpecialViewTypes).contains(folder.getType());

            if (showSpecialViews) {
                if (folder.getType() != BaseItemKind.CHANNEL_FOLDER_ITEM) {
                    StdItemQuery resume = new StdItemQuery();
                    resume.setParentId(folder.getId().toString());
                    resume.setLimit(50);
                    resume.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
                    resume.setSortBy(new String[]{ItemSortBy.DatePlayed});
                    resume.setSortOrder(SortOrder.Descending);
                    mRows.add(new BrowseRowDef(getString(R.string.lbl_continue_watching), resume, 0));
                }

                StdItemQuery latest = new StdItemQuery();
                latest.setParentId(folder.getId().toString());
                latest.setLimit(50);
                latest.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
                latest.setSortBy(new String[]{ItemSortBy.DateCreated});
                latest.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(getString(R.string.lbl_latest_additions), latest, 0));

            }

            StdItemQuery byName = new StdItemQuery();
            byName.setParentId(folder.getId().toString());
            String header = (folder.getType() == BaseItemKind.SEASON) ? folder.getName() : getString(R.string.lbl_by_name);
            mRows.add(new BrowseRowDef(header, byName, 100));

            rowLoader.loadRows(mRows);
        }
    }
}
