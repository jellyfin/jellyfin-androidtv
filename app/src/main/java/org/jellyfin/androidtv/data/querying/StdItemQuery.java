package org.jellyfin.androidtv.data.querying;

import org.jellyfin.androidtv.auth.UserRepository;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemQuery;
import org.koin.java.KoinJavaComponent;

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
        setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        setFields(fields);
    }

    public StdItemQuery() {
        this(null);
    }
}
