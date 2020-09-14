package org.jellyfin.androidtv.data.model;

import org.jellyfin.androidtv.ui.itemdetail.FullDetailsActivity;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

import timber.log.Timber;

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
        Timber.e(exception, "Error retrieving full object");
    }
}
