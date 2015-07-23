package tv.emby.embyatv.itemhandling;

import android.os.Handler;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.net.HttpException;
import mediabrowser.model.querying.ItemsResult;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 4/1/2015.
 */
public class ItemQueryResponse extends Response<ItemsResult> {

    private ItemRowAdapter adapter;

    public ItemQueryResponse(ItemRowAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onResponse(ItemsResult response) {
        if (response.getTotalRecordCount() > 0) {
            int i = adapter.getItemsLoaded();
            if (i == 0 && adapter.size() > 0) adapter.clear();
            for (BaseItemDto item : response.getItems()) {
                adapter.add(new BaseRowItem(i++, item, adapter.getPreferParentThumb(), adapter.isStaticHeight()));
            }
            adapter.setTotalItems(response.getTotalRecordCount());
            adapter.setItemsLoaded(i);
            if (i == 0) adapter.removeRow();
        } else {
            // no results - don't show us
            adapter.removeRow();
        }

        adapter.setCurrentlyRetrieving(false);
    }

    @Override
    public void onError(Exception exception) {
        TvApp.getApplication().getLogger().ErrorException("Error retrieving items", exception);
        if (exception instanceof HttpException) {
            HttpException httpException = (HttpException) exception;
            if (httpException.getStatusCode() != null && httpException.getStatusCode() == 401 && "ParentalControl".equals(httpException.getHeaders().get("X-Application-Error-Code"))) {
                Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_access_restricted));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        System.exit(1);
                    }
                }, 3000);
            } else {
                adapter.removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
            }
        } else {
            adapter.removeRow();
            Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());

        }
        adapter.setCurrentlyRetrieving(false);
    }

}
