package org.jellyfin.androidtv.ui.playback.overlay.actions;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;

public class RecordAction extends CustomAction {

    public static final int INDEX_INACTIVE = 0;
    public static final int INDEX_RECORDING = 1;


    public RecordAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        Drawable recordInactive = context.getDrawable(R.drawable.ic_record);
        Drawable recordActive = context.getDrawable(R.drawable.ic_record_red);
        setIndex(INDEX_INACTIVE);
        setDrawables(new Drawable[]{recordInactive, recordActive});

    }
}
