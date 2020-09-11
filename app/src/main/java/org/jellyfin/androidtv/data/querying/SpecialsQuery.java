package org.jellyfin.androidtv.data.querying;

/**
 * Created by Eric on 2/12/2015.
 */
public class SpecialsQuery {
    private String ItemId;

    public SpecialsQuery(String itemId) {
        ItemId = itemId;
    }

    public String getItemId() {
        return ItemId;
    }

}
