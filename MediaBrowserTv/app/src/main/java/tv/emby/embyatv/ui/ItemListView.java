package tv.emby.embyatv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;

/**
 * Created by Eric on 11/21/2015.
 */
public class ItemListView extends FrameLayout {
    Context mContext;
    LinearLayout mList;
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

    public void addItems(BaseItemDto[] items) {
        int i = 0;
        for (BaseItemDto item : items) {
            addItem(item, i++);
        }
        //Throw in another item just to provide some padding at the end of the scroll
        mList.addView(new TextView(mContext));

    }

    public void addItem(BaseItemDto item, int ndx) {
        mList.addView(new ItemRowView(mContext, item, ndx, mRowSelectedListener, mRowClickedListener));
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
}
