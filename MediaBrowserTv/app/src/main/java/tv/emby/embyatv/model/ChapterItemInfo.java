package tv.emby.embyatv.model;

import mediabrowser.model.entities.ChapterInfo;

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
