package org.jellyfin.androidtv.data.querying;

/**
 * Created by Eric on 2/12/2015.
 */
public class TrailersQuery {
    private String ItemId;

    public TrailersQuery(String itemId) {
        ItemId = itemId;
    }

    public String getItemId() {
        return ItemId;
    }

}
