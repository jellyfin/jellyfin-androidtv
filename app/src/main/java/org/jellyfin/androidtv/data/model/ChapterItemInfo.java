package org.jellyfin.androidtv.data.model;

import org.jellyfin.apiclient.model.entities.ChapterInfo;

/**
 * Created by Eric on 2/12/2015.
 */
public class ChapterItemInfo extends ChapterInfo {
    private String itemId;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
}
