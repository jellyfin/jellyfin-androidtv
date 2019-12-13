package org.jellyfin.androidtv.browsing;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.model.ChangeTriggerType;
import org.jellyfin.androidtv.presentation.CardPresenter;
import org.jellyfin.apiclient.model.configuration.UserConfiguration;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.querying.LatestItemsQuery;

import java.util.Arrays;
import java.util.List;

import androidx.leanback.widget.ArrayObjectAdapter;

class HomeFragmentLatestRow extends HomeFragmentRow {
    private final ItemsResult views;

    public HomeFragmentLatestRow(ItemsResult views) {
        this.views = views;
    }

    @Override
    public void addToRowsAdapter(CardPresenter cardPresenter, ArrayObjectAdapter rowsAdapter) {
        TvApp application = TvApp.getApplication();

        // Get configuration (to find excluded items)
        UserConfiguration configuration = application.getCurrentUser().getConfiguration();

        // Create a list of views to include
        List<String> latestItemsExcludes = Arrays.asList(configuration.getLatestItemsExcludes());

        for (BaseItemDto item : views.getItems()) {
            if (latestItemsExcludes.contains(item.getId())) continue;// Skip excluded items

            // Create query and add row
            LatestItemsQuery query = new LatestItemsQuery();
            query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.Overview});
            query.setImageTypeLimit(1);
            query.setParentId(item.getId());
            query.setGroupItems(true);
            query.setLimit(50);

            HomeFragmentBrowseRowDefRow row = new HomeFragmentBrowseRowDefRow(new BrowseRowDef(String.format("%s %s", application.getString(R.string.lbl_latest), item.getName()), query, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated}));
            row.addToRowsAdapter(cardPresenter, rowsAdapter);
        }
    }
}
