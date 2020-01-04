package org.jellyfin.androidtv.playback.overlay.actions;

import android.content.Context;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.playback.overlay.CustomPlaybackTransportControlGlue;

public class ClosedCaptionsAction extends CustomAction {

    public ClosedCaptionsAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        initializeWithIcon(R.drawable.ic_select_subtitle);
    }
}
