package org.jellyfin.androidtv.ui.presentation;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowPresenter;

public class CustomListRowPresenter extends ListRowPresenter {
    private View viewHolder;
    private Integer topPadding;
    private Drawable backgroundDrawable;

    public CustomListRowPresenter() {
        super();

        setHeaderPresenter(new CustomRowHeaderPresenter());
    }

    public CustomListRowPresenter(Integer topPadding) {
        super();
        this.topPadding = topPadding;

        setHeaderPresenter(new CustomRowHeaderPresenter());
    }

    public CustomListRowPresenter(Drawable drawable, Integer topPadding) {
        super();
        this.topPadding = topPadding;
        this.backgroundDrawable = drawable;

        setHeaderPresenter(new CustomRowHeaderPresenter());
    }

    @Override
    public boolean isUsingDefaultShadow() {
        return false;
    }

    @Override
    protected void onSelectLevelChanged(RowPresenter.ViewHolder holder) {
        // Do nothing - this removes the shadow on the out of focus rows of image cards
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);

        viewHolder = (View) holder.view.getParent();

        if (topPadding != null) {
            viewHolder.setPadding(viewHolder.getPaddingLeft(), topPadding, viewHolder.getPaddingRight(), viewHolder.getPaddingBottom());
        }

        if (backgroundDrawable != null) {
            viewHolder.setBackground(backgroundDrawable);
        }
    }
}
