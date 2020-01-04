package org.jellyfin.androidtv.playback.overlay.actions;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.playback.overlay.CustomPlaybackTransportControlGlue;

public class AdjustAudioDelayAction extends CustomAction {

    public AdjustAudioDelayAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        Drawable drawable = context.getDrawable(R.drawable.ic_adjust);
        setIcon(drawable);
        setDrawables(new Drawable[]{drawable});
    }
}
