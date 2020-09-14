package org.jellyfin.androidtv.data.model;

import org.jellyfin.apiclient.model.dto.BaseItemDto;

import java.util.Arrays;
import java.util.List;

public class BaseItemList {
    private List<BaseItemDto> list;

    public BaseItemList(List<BaseItemDto> items) {
        list = items;
    }

    public BaseItemList(BaseItemDto[] items) {
        list = Arrays.asList(items);
    }

    public List<BaseItemDto> getItems() {
        return list;
    }
}
