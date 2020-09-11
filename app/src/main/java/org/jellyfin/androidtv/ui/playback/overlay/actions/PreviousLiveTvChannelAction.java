package org.jellyfin.androidtv.ui.playback.overlay.actions;

import android.content.Context;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;

public class PreviousLiveTvChannelAction extends CustomAction {

    public PreviousLiveTvChannelAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        super.initializeWithIcon(R.drawable.ic_previous_episode);
    }
}
