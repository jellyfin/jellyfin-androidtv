package org.jellyfin.androidtv.playback.overlay.actions;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.playback.overlay.CustomPlaybackTransportControlGlue;

public class ZoomAction extends CustomAction {

    public ZoomAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        Drawable drawable = context.getDrawable(R.drawable.ic_zoom);
        setIcon(drawable);
        setDrawables(new Drawable[]{drawable});
    }
}
