package org.jellyfin.androidtv.data.querying;

import org.jellyfin.androidtv.TvApp;

import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemQuery;

public class StdItemQuery extends ItemQuery {
    public StdItemQuery(ItemFields[] fields) {
        if (fields == null) {
            fields = new ItemFields[]{
                    ItemFields.PrimaryImageAspectRatio,
                    ItemFields.Overview,
                    ItemFields.ItemCounts,
                    ItemFields.DisplayPreferencesId,
                    ItemFields.ChildCount
            };
        }
        setUserId(TvApp.getApplication().getCurrentUser().getId());
        setFields(fields);
    }

    public StdItemQuery() {
        this(null);
    }
}
