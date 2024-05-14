package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.jellyfin.androidtv.databinding.ItemListBinding;
import org.jellyfin.sdk.model.api.BaseItemDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemListView extends FrameLayout {
    Context mContext;
    LinearLayout mList;
    List<UUID> mItemIds = new ArrayList<>();
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

    public void addItem(BaseItemDto item, int ndx) {
        mList.addView(new ItemRowView(mContext, item, ndx, mRowSelectedListener, mRowClickedListener));
        mItemIds.add(item.getId());
    }

    public ItemRowView updatePlaying(UUID id) {
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
}
