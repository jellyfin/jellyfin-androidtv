package org.jellyfin.androidtv.ui.browsing;

import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import java.util.ArrayList;
import java.util.List;

public class CompositeSelectedListener implements OnItemViewSelectedListener {
    private List<OnItemViewSelectedListener> registeredListeners = new ArrayList<>();

    public void registerListener (OnItemViewSelectedListener listener) {
        registeredListeners.add(listener);
    }

    public void unRegisterListener (OnItemViewSelectedListener listener) {
        registeredListeners.remove(listener);
    }


    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        for (OnItemViewSelectedListener listener : registeredListeners) {
            listener.onItemSelected(itemViewHolder, item, rowViewHolder, row);
        }
    }
}
