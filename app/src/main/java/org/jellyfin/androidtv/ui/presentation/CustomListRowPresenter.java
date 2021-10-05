package org.jellyfin.androidtv.ui.presentation;

import android.graphics.drawable.Drawable;

import androidx.leanback.widget.BaseGridView;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ListRowView;
import androidx.leanback.widget.RowPresenter;

import android.view.View;
import android.view.ViewGroup;

import org.jellyfin.androidtv.R;

public class CustomListRowPresenter extends ListRowPresenter {
    private View viewHolder;
    private Integer topPadding;
    private Drawable backgroundDrawable;
    private boolean homeHeaderEnabled = false;

    public CustomListRowPresenter() {
        super();

        setHeaderPresenter(new CustomRowHeaderPresenter());
    }

    public CustomListRowPresenter(Integer topPadding) {
        super();
        this.topPadding = topPadding;

        setHeaderPresenter(new CustomRowHeaderPresenter());
    }

    public CustomListRowPresenter(boolean homeHeaderEnabled) {
        super();
        this.homeHeaderEnabled = homeHeaderEnabled;

        setHeaderPresenter(new CustomRowHeaderPresenter(homeHeaderEnabled));
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

    @Override
    protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
        RowPresenter.ViewHolder viewHolder = super.createRowViewHolder(parent);
        HorizontalGridView gridView = ((ListRowView) viewHolder.view).getGridView();

        if (homeHeaderEnabled) {
            gridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_LOW_EDGE);
            gridView.setWindowAlignmentOffsetPercent(0f);
            gridView.setWindowAlignmentOffset(parent.getResources().getDimensionPixelSize(R.dimen.lb_browse_padding_start));
            gridView.setItemAlignmentOffsetPercent(0f);
        }

        return viewHolder;
    }
}
