package org.jellyfin.androidtv.customer;

import com.alibaba.fastjson.JSON;

import org.jellyfin.androidtv.danmu.model.AutoSkipModel;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class CustomerUserPreferencesTest {

    @Test
    void saveTest() {
        Map<String, AutoSkipModel> itemAutoSkipTimes = new HashMap<>();
        AutoSkipModel autoSkipModel = new AutoSkipModel();
        autoSkipModel.setId("2341241");
        autoSkipModel.setcTime(12131233L);
        itemAutoSkipTimes.put(autoSkipModel.getId(), autoSkipModel);
        autoSkipModel.setId("b2341241");
        autoSkipModel.setcTime(12131234L);
        itemAutoSkipTimes.put(autoSkipModel.getId(), autoSkipModel);
        System.out.println(JSON.toJSONString(itemAutoSkipTimes.values()));
    }
}
