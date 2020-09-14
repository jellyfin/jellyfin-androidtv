package org.jellyfin.androidtv.ui.browsing;

import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

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

