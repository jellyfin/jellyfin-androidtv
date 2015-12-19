package tv.emby.embyatv.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Eric on 11/21/2015.
 */
public class BaseItemList {
    private List<BaseItemDto> list;

    public BaseItemList(List<BaseItemDto> items) {
        list = items;
    }

    public BaseItemList(BaseItemDto[] items) {
        list = Arrays.asList(items);
    }

    public List<BaseItemDto> getItems() { return list; }
}
