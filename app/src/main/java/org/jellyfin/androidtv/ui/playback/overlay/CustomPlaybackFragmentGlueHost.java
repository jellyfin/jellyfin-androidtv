package org.jellyfin.androidtv.ui.playback.overlay;

import androidx.leanback.app.PlaybackSupportFragment;
import androidx.leanback.app.PlaybackSupportFragmentGlueHost;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import org.jellyfin.androidtv.ui.playback.overlay.action.CustomAction;

public class CustomPlaybackFragmentGlueHost extends PlaybackSupportFragmentGlueHost {

    private final PlaybackSupportFragment fragment;

    CustomPlaybackFragmentGlueHost(PlaybackSupportFragment fragment) {
        super(fragment);
        this.fragment = fragment;
    }

    @Override
    public void setOnActionClickedListener(OnActionClickedListener listener) {
        // Copy from superclass
        if (listener == null) {
            fragment.setOnPlaybackItemViewClickedListener(null);
        } else {
            fragment.setOnPlaybackItemViewClickedListener(new OnItemViewClickedListener() {
                @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                          RowPresenter.ViewHolder rowViewHolder, Row row) {
                    // Call our custom function and pass the view instance
                    if (item instanceof CustomAction) {
                        ((CustomAction) item).onCustomActionClicked(itemViewHolder.view);
                    }
                    if (item instanceof Action) {
                        listener.onActionClicked((Action) item);
                    }
                }
            });
        }
    }
}
