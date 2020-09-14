package org.jellyfin.androidtv.data.model;

import java.util.Arrays;
import java.util.List;

import org.jellyfin.apiclient.model.dto.BaseItemDto;

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
