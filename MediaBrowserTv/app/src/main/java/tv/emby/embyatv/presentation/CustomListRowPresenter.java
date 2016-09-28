package tv.emby.embyatv.presentation;

import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.View;

/**
 * Created by Eric on 10/12/2015.
 */
public class CustomListRowPresenter extends ListRowPresenter {

    private View viewHolder;
    private int backgroundColor = 0;
    private Drawable backgroundDrawable;

    public CustomListRowPresenter(int color) {
        super();
        this.backgroundColor = color;
    }

    public CustomListRowPresenter(Drawable drawable) {
        super();
        this.backgroundDrawable = drawable;
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);

        viewHolder = (View) holder.view.getParent();
        if (backgroundDrawable != null) {
            viewHolder.setBackground(backgroundDrawable);
        } else {
            viewHolder.setBackgroundColor(backgroundColor);
        }
    }


}
