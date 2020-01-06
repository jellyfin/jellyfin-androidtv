package org.jellyfin.androidtv.playback.overlay;

import android.view.ViewGroup;

import androidx.leanback.media.PlaybackBaseControlGlue;
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;
import androidx.leanback.widget.Presenter;

public class FullDescriptionPresenter extends AbstractDetailsDescriptionPresenter {


    @Override
    protected void onBindDescription(ViewHolder vh, Object item) {
        PlaybackBaseControlGlue glue = (PlaybackBaseControlGlue) item;
        vh.getTitle().setText(glue.getTitle());
        vh.getBody().setText(glue.getSubtitle());
    }

}
