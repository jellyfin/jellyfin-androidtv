package org.jellyfin.androidtv.ui.home;

import android.content.Context;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;

class HomeFragmentNowPlayingRow extends HomeFragmentRow {
    private final Context context;
    private ListRow row;

    public HomeFragmentNowPlayingRow(Context context) {
        this.context = context;
    }

    @Override
    public void addToRowsAdapter(CardPresenter cardPresenter, ArrayObjectAdapter rowsAdapter) {
        update(rowsAdapter);
    }

    private void add(ArrayObjectAdapter rowsAdapter) {
        if (row == null) {
            row = new ListRow(new HeaderItem(context.getString(R.string.lbl_now_playing)), MediaManager.getManagedAudioQueue());

            rowsAdapter.add(0, row);
        }
    }

    private void remove(ArrayObjectAdapter rowsAdapter) {
        if (row != null) {
            rowsAdapter.remove(row);
            row = null;
        }
    }

    public void update(ArrayObjectAdapter rowsAdapter) {
        if (MediaManager.isPlayingAudio()) {
            add(rowsAdapter);
        } else {
            remove(rowsAdapter);
        }
    }
}
