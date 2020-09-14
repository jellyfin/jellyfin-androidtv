package org.jellyfin.androidtv.ui.playback.overlay.action;

import android.content.Context;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;

public class GuideAction extends CustomAction {

    public GuideAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        super.initializeWithIcon(R.drawable.ic_guide);
    }
}
