package org.jellyfin.androidtv.details;

import org.jellyfin.androidtv.TvApp;

import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

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
