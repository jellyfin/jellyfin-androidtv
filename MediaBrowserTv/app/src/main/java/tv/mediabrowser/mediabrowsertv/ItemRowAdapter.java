package tv.mediabrowser.mediabrowsertv;

import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemsResult;

/**
 * Created by Eric on 12/5/2014.
 */
public class ItemRowAdapter extends ArrayObjectAdapter {
    private ItemQuery mQuery;

    public ItemRowAdapter(ItemQuery query, Presenter presenter) {
        super(presenter);
        mQuery = query;
    }

    public void Retrieve() {
        Retrieve(mQuery);
    }

    public void Retrieve(ItemQuery query) {
        final ItemRowAdapter adapter = this;
            TvApp.getApplication().getConnectionManager().GetApiClient(TvApp.getApplication().getCurrentUser()).GetItemsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                for (BaseItemDto item : response.getItems()) {
                    adapter.add(item);
                }
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving items", exception);
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
            }
        });

    }
}
