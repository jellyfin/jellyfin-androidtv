package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.databinding.ItemListBinding;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.List;

public class ItemListView extends FrameLayout {
    Context mContext;
    LinearLayout mList;
    List<String> mItemIds = new ArrayList<>();
    ItemRowView.RowSelectedListener mRowSelectedListener;
    ItemRowView.RowClickedListener mRowClickedListener;

    public ItemListView(Context context) {
        super(context);
        inflateView(context);
    }

    public ItemListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        inflateView(context);
    }

    private void inflateView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        ItemListBinding binding = ItemListBinding.inflate(inflater, this, true);
        mContext = context;
        mList = binding.songList;
    }

    public void setRowSelectedListener(ItemRowView.RowSelectedListener listener) { mRowSelectedListener = listener; }
    public void setRowClickedListener(ItemRowView.RowClickedListener listener) { mRowClickedListener = listener; }

    public void clear() {
        mList.removeAllViews();
        mItemIds.clear();
    }

    public void addItems(List<BaseItemDto> items) {
        int i = 0;
        for (BaseItemDto item : items) {
            addItem(item, i++);
        }
    }

    public void addItem(BaseItemDto item, int ndx) {
        mList.addView(new ItemRowView(mContext, item, ndx, mRowSelectedListener, mRowClickedListener));
        mItemIds.add(item.getId());
    }

    public ItemRowView updatePlaying(String id) {
        //look through our song rows and update the playing indicator
        ItemRowView ret = null;
        for (int i = 0; i < mList.getChildCount(); i++) {
            View view = mList.getChildAt(i);
            if (view instanceof ItemRowView) {
                ItemRowView row = (ItemRowView)view;
                if (row.setPlaying(id)) ret = row;
            }
        }
        return ret;
    }

    public void refresh() {
        //update watched state for all items
        //get them in batch for better performance
        StdItemQuery query = new StdItemQuery(new ItemFields[] {
                ItemFields.MediaSources,
                ItemFields.ChildCount
        });
        query.setUserId(TvApp.getApplication().getCurrentUser().getId().toString());
        String[] ids = new String[mItemIds.size()];
        query.setIds(mItemIds.toArray(ids));
        KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null) {
                    int i = 0;
                    for (BaseItemDto item : response.getItems()) {
                        // we have title view as first one
                        View view = mList.getChildAt(i + 1);
                        if (view instanceof ItemRowView) {
                            ItemRowView row = (ItemRowView) view;
                            row.setItem(item, i++);
                        }
                    }
                }
            }
        });
    }
}
