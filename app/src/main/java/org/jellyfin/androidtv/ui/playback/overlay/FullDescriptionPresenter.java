package org.jellyfin.androidtv.ui.playback.overlay;

import androidx.leanback.media.PlaybackBaseControlGlue;
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;

public class FullDescriptionPresenter extends AbstractDetailsDescriptionPresenter {


    @Override
    protected void onBindDescription(ViewHolder vh, Object item) {
        PlaybackBaseControlGlue glue = (PlaybackBaseControlGlue) item;
        vh.getTitle().setText(glue.getTitle());
        vh.getBody().setText(glue.getSubtitle());
    }

}
