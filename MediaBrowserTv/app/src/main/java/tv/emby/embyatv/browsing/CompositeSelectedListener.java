package tv.emby.embyatv.browsing;

import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;

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
