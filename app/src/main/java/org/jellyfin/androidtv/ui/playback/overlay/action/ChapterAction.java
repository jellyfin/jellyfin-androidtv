package org.jellyfin.androidtv.ui.playback.overlay.action;

import android.content.Context;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;

public class ChapterAction extends CustomAction {

    public ChapterAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        initializeWithIcon(R.drawable.ic_select_chapter);
    }
}
