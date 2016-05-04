package tv.emby.embyatv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemsResult;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.querying.StdItemQuery;

/**
 * Created by Eric on 11/21/2015.
 */
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
        inflater.inflate(R.layout.item_list, this);
        mContext = context;
        mList = (LinearLayout) findViewById(R.id.songList);

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
        //Throw in another item just to provide some padding at the end of the scroll
        mList.addView(new TextView(mContext));

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
        StdItemQuery query = new StdItemQuery(new ItemFields[] {ItemFields.MediaSources});
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        String[] ids = new String[mItemIds.size()];
        query.setIds(mItemIds.toArray(ids));
        TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null) {
                    int i = 0;
                    for (BaseItemDto item : response.getItems()) {
                        View view = mList.getChildAt(i+1); // we have title view as first one
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
