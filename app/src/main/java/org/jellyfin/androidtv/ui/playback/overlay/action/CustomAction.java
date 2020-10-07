package org.jellyfin.androidtv.ui.playback.overlay.action;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.PlaybackControlsRow;

import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment;

public abstract class CustomAction extends PlaybackControlsRow.MultiAction {

    private Context context;
    private CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue;

    public CustomAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(0);
        this.context = context;
        this.customPlaybackTransportControlGlue = customPlaybackTransportControlGlue;
    }

    public void onCustomActionClicked(View view) { // We need a custom onClicked implementation for showing the popup
        customPlaybackTransportControlGlue.onCustomActionClicked(this, view);
    }

    void initializeWithIcon(int resourceId) {
        Drawable drawable = ContextCompat.getDrawable(context, resourceId);
        setIcon(drawable);
        setDrawables(new Drawable[]{drawable});
    }

    public void handleClickAction(PlaybackController playbackController, LeanbackOverlayFragment leanbackOverlayFragment, Context context, View view) {
    }
}
