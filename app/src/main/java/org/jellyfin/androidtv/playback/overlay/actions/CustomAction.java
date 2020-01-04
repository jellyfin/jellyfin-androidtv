package org.jellyfin.androidtv.playback.overlay.actions;


import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.leanback.widget.PlaybackControlsRow;

abstract class CustomAction extends PlaybackControlsRow.MultiAction {

    Context context;

    public CustomAction(Context context) {
        this(0);
        this.context = context;
    }

    private CustomAction(int id) {
        super(id);
    }

}
