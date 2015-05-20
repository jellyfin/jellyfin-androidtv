package tv.emby.embyatv.details;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 3/23/2015.
 */
public class DetailItemLoadResponse extends Response<BaseItemDto> {
    private BaseItemDetailsFragment detailFragment;

    public DetailItemLoadResponse(BaseItemDetailsFragment fragment) {
        this.detailFragment = fragment;
    }

    @Override
    public void onResponse(BaseItemDto response) {
        detailFragment.setBaseItem(response);
    }

    @Override
    public void onError(Exception exception) {
        TvApp.getApplication().getLogger().ErrorException("Error retrieving full object", exception);
    }
}
