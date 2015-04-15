package tv.emby.embyatv.browsing;

import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eric on 4/15/2015.
 */
public class CompositeClickedListener implements OnItemViewClickedListener {
    private List<OnItemViewClickedListener> registeredListeners = new ArrayList<>();

    public void registerListener (OnItemViewClickedListener listener) {
        registeredListeners.add(listener);
    }

    public void unRegisterListener (OnItemViewClickedListener listener) {
        registeredListeners.remove(listener);
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        for (OnItemViewClickedListener listener : registeredListeners) {
            listener.onItemClicked(itemViewHolder, item, rowViewHolder, row);
        }
    }
}

