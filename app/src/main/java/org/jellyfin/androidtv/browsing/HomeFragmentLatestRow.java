package org.jellyfin.androidtv.browsing;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.model.ChangeTriggerType;
import org.jellyfin.androidtv.presentation.CardPresenter;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.configuration.UserConfiguration;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.UserDto;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.querying.LatestItemsQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.leanback.widget.ArrayObjectAdapter;

class HomeFragmentLatestRow extends HomeFragmentRow {
    @Override
    public void addToRowsAdapter(final CardPresenter cardPresenter, final ArrayObjectAdapter rowsAdapter) {
        final TvApp application = TvApp.getApplication();

        // Get configuration (to find excluded items)
        UserDto user = application.getCurrentUser();
        final UserConfiguration configuration = user.getConfiguration();

        // Get user views
        application.getApiClient().GetUserViews(user.getId(), new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                // Create a list of views to include
                List<String> latestItemsExcludes = Arrays.asList(configuration.getLatestItemsExcludes());
                List<BaseItemDto> items = new ArrayList<>();

                for (BaseItemDto item : response.getItems()) {
                    if (!latestItemsExcludes.contains(item.getId())) {// Skip excluded items
                        items.add(item);
                    }
                }

                // Add rows
                addLibraries(items, cardPresenter, rowsAdapter, application);
            }
        });
    }

    //todo: As this function is called in a asynchronous callback the rows will always be inserted at the bottom of the home fragment
    public void addLibraries(List<BaseItemDto> items, CardPresenter cardPresenter, ArrayObjectAdapter rowsAdapter, TvApp application) {
        for (BaseItemDto item : items) {
            LatestItemsQuery query = new LatestItemsQuery();
            query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.Overview});
            query.setImageTypeLimit(1);
            query.setParentId(item.getId());
            query.setLimit(50);

            HomeFragmentBrowseRowDefRow row = new HomeFragmentBrowseRowDefRow(new BrowseRowDef(String.format("%s %s", application.getString(R.string.lbl_latest), item.getName()), query, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated}));
            row.addToRowsAdapter(cardPresenter, rowsAdapter);
        }
    }
}
