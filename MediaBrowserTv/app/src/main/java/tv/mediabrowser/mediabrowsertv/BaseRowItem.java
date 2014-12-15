package tv.mediabrowser.mediabrowsertv;

import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Eric on 12/15/2014.
 */
public class BaseRowItem {
    private int index;
    private BaseItemDto baseItem;

    public BaseRowItem(int index, BaseItemDto item) {
        this.index = index;
        this.baseItem = item;
    }

    public int getIndex() {
        return index;
    }

    public BaseItemDto getBaseItem() {
        return baseItem;
    }

}
