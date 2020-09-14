package org.jellyfin.androidtv.ui.presentation;

import android.graphics.drawable.Drawable;

import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowPresenter;

import timber.log.Timber;

public class PositionableListRowPresenter extends CustomListRowPresenter implements IPositionablePresenter {

    private ListRowPresenter.ViewHolder viewHolder;

    public PositionableListRowPresenter() { super(); }

    public PositionableListRowPresenter(Drawable background, Integer padding) {
        super(background, padding);
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);

        viewHolder = (ViewHolder) holder;
    }

    public void setPosition(int ndx) {
        Timber.d("Setting position to: %d", ndx);
        if (viewHolder != null && viewHolder.getGridView() != null) viewHolder.getGridView().setSelectedPosition(ndx);
    }

    public int getPosition() {
        return viewHolder != null && viewHolder.getGridView() != null ? viewHolder.getGridView().getSelectedPosition() : -1;
    }
}
