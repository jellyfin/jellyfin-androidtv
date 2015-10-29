package tv.emby.embyatv.presentation;

import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.RowPresenter;

import tv.emby.embyatv.TvApp;

/**
 * Created by Eric on 10/12/2015.
 */
public class PositionableListRowPresenter extends ListRowPresenter implements IPositionablePresenter {

    private ListRowPresenter.ViewHolder viewHolder;
    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);

        viewHolder = (ViewHolder) holder;
    }

    public void setPosition(int ndx) {
        TvApp.getApplication().getLogger().Debug("Setting position to: "+ndx);
        if (viewHolder != null && viewHolder.getGridView() != null) viewHolder.getGridView().setSelectedPosition(ndx);
    }

    public int getPosition() {
        return viewHolder != null && viewHolder.getGridView() != null ? viewHolder.getGridView().getSelectedPosition() : -1;
    }

}
