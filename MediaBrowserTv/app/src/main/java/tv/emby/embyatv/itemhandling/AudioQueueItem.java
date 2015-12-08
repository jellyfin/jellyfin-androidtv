package tv.emby.embyatv.itemhandling;

import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Eric on 12/8/2015.
 */
public class AudioQueueItem extends BaseRowItem {
    public AudioQueueItem(int index, BaseItemDto item) {
        super(index, item);
        this.staticHeight = true;
    }
}
