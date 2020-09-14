package org.jellyfin.androidtv.ui.presentation;

import android.graphics.Color;
import androidx.leanback.widget.Presenter;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

public class TextItemPresenter extends Presenter {
    private static final int ITEM_WIDTH = 400;
    private static final int ITEM_HEIGHT = 200;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        TextView view = new TextView(parent.getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(ITEM_WIDTH, ITEM_HEIGHT));
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setTextSize(32);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((TextView) viewHolder.view).setText(item.toString());
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
    }
}
