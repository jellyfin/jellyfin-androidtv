package tv.mediabrowser.mediabrowsertv;

import android.support.v17.leanback.widget.ArrayObjectAdapter;
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
    private ArrayObjectAdapter mParent;
    private ListRow mRow;
    private int chunkSize = 0;

    private long itemsLoaded = 0;
    private long totalItems = 0;
    private boolean fullyLoaded = false;
    private boolean currentlyRetrieving = false;

    public ItemRowAdapter(ItemQuery query, int chunkSize, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mQuery = query;
        this.chunkSize = chunkSize;
        queryType = QueryType.Items;
    }

    public ItemRowAdapter(NextUpQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mNextUpQuery = query;
        queryType = QueryType.NextUp;
    }

    public ItemRowAdapter(ViewQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        queryType = QueryType.Views;
    }

    public void setItemsLoaded(long itemsLoaded) {
        this.itemsLoaded = itemsLoaded;
        this.fullyLoaded = chunkSize == 0 || itemsLoaded >= totalItems;
    }

    public long getItemsLoaded() {
        return itemsLoaded;
    }

    public void loadMoreItemsIfNeeded(long pos) {
        if (fullyLoaded) {
            TvApp.getApplication().getLogger().Debug("Row is fully loaded");
            return;
        }
        if (currentlyRetrieving) {
            TvApp.getApplication().getLogger().Debug("Not loading more because currently retrieving");
            return;
        }

        if (pos >= itemsLoaded - 20) {
            //TODO load more...
            TvApp.getApplication().getLogger().Debug("Would load more items...");
        }

    }

    public void Retrieve() {
        currentlyRetrieving = true;
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
                if (response.getTotalRecordCount() > 0) {
                    int i = 0;
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++,item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                } else {
                    // no results - don't show us
                    mParent.remove(mRow);
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving items", exception);
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void Retrieve(ItemQuery query) {
        final ItemRowAdapter adapter = this;
            TvApp.getApplication().getConnectionManager().GetApiClient(TvApp.getApplication().getCurrentUser()).GetItemsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getTotalRecordCount() > 0) {
                    int i = 0;
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++,item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                } else {
                    // no results - don't show us
                    mParent.remove(mRow);
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving items", exception);
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }
    public void Retrieve(NextUpQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getConnectionManager().GetApiClient(TvApp.getApplication().getCurrentUser()).GetNextUpEpisodesAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getTotalRecordCount() > 0) {
                    int i = 0;
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++,item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                } else {
                    // no results - don't show us
                    mParent.remove(mRow);
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving next up items", exception);
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void setRow(ListRow row) {
        mRow = row;
    }
}
