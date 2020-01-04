package org.jellyfin.androidtv.playback.overlay.actions;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.playback.overlay.CustomPlaybackTransportControlGlue;

public class SelectAudioAction extends CustomAction {

    public SelectAudioAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        Drawable drawable = context.getDrawable(R.drawable.ic_select_audio);
        setIcon(drawable);
        setDrawables(new Drawable[]{drawable});
    }
}
