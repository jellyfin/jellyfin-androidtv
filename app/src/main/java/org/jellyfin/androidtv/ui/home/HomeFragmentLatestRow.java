package org.jellyfin.androidtv.ui.home;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.browsing.BrowseRowDef;
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
    // See: https://github.com/jellyfin/jellyfin-web/blob/bbf1f8d5df66a58c29f07969caa476852d86ab4a/src/components/homesections/homesections.js#L292
    // Added books since they are currently broken and no plans on adding support
    private static final List<String> EXCLUDED_COLLECTION_TYPES = Arrays.asList("playlists", "livetv", "boxsets", "channels", "books");

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
            // Skip excluded collection types
            if (EXCLUDED_COLLECTION_TYPES.contains(item.getCollectionType())) continue;
            // Skip user configured excluded items
            if (latestItemsExcludes.contains(item.getId())) continue;

            // Create query and add row
            LatestItemsQuery query = new LatestItemsQuery();
            query.setFields(new ItemFields[]{
                    ItemFields.PrimaryImageAspectRatio,
                    ItemFields.Overview,
                    ItemFields.ChildCount
            });
            query.setImageTypeLimit(1);
            query.setParentId(item.getId());
            query.setGroupItems(true);
            query.setLimit(50);

            HomeFragmentBrowseRowDefRow row = new HomeFragmentBrowseRowDefRow(new BrowseRowDef(String.format("%s %s", application.getString(R.string.lbl_latest), item.getName()), query, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated}));
            row.addToRowsAdapter(cardPresenter, rowsAdapter);
        }
    }
}
