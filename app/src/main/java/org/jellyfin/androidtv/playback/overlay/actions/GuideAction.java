package org.jellyfin.androidtv.playback.overlay.actions;

import android.content.Context;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.playback.overlay.CustomPlaybackTransportControlGlue;

public class GuideAction extends CustomAction {

    public GuideAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        super.initializeWithIcon(R.drawable.ic_guide);
    }
}
