package org.jellyfin.androidtv.details;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.util.Utils;

/**
 * Created by Eric on 3/23/2015.
 */
public class DetailItemLoadResponse extends Response<BaseItemDto> {
    private FullDetailsActivity activity;

    public DetailItemLoadResponse(FullDetailsActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onResponse(BaseItemDto response) {
        activity.setBaseItem(response);
    }

    @Override
    public void onError(Exception exception) {
        TvApp.getApplication().getLogger().ErrorException("Error retrieving full object", exception);
    }
}
