package tv.emby.embyatv.presentation;

import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.BaseCardView;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.View;
import android.view.ViewGroup;

import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 10/12/2015.
 */
public class CustomListRowPresenter extends ListRowPresenter {

    private View viewHolder;
    private Integer backgroundColor;
    private Integer topPadding;
    private Drawable backgroundDrawable;

    public CustomListRowPresenter(int color) {
        super();
        this.backgroundColor = color;
    }

    public CustomListRowPresenter(Drawable drawable, Integer topPadding) {
        super();
        this.topPadding = topPadding;
        this.backgroundDrawable = drawable;
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
        } else if (backgroundColor != null) {
            viewHolder.setBackgroundColor(backgroundColor);
        }
    }


}
