package tv.mediabrowser.mediabrowsertv;

import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.NextUpQuery;

/**
 * Created by Eric on 12/5/2014.
 */
public class ItemRowAdapter extends ArrayObjectAdapter {
    private ItemQuery mQuery;
    private NextUpQuery mNextUpQuery;
    private QueryType queryType;

    public ItemRowAdapter(ItemQuery query, Presenter presenter) {
        super(presenter);
        mQuery = query;
        queryType = QueryType.Items;
    }

    public ItemRowAdapter(NextUpQuery query, Presenter presenter) {
        super(presenter);
        mNextUpQuery = query;
        queryType = QueryType.NextUp;
    }

    public ItemRowAdapter(ViewQuery query, Presenter presenter) {
        super(presenter);
        queryType = QueryType.Views;
    }

    public void Retrieve() {
        switch (queryType) {
            case Items:
                Retrieve(mQuery);
                break;
            case NextUp:
                Retrieve(mNextUpQuery);
                break;
            case Views:
                RetrieveViews();
                break;
        }
    }

    private void RetrieveViews() {
        final ItemRowAdapter adapter = this;
        UserDto user = TvApp.getApplication().getCurrentUser();
        TvApp.getApplication().getConnectionManager().GetApiClient(user).GetUserViews(user.getId(), new Response<ItemsResult>() {
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
    public void Retrieve(NextUpQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getConnectionManager().GetApiClient(TvApp.getApplication().getCurrentUser()).GetNextUpEpisodesAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                for (BaseItemDto item : response.getItems()) {
                    adapter.add(item);
                }
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving next up items", exception);
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
            }
        });

    }
}
