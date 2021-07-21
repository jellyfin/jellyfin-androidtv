package org.jellyfin.androidtv.ui.playback.overlay.action;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;

public class RecordAction extends CustomAction {

    public static final int INDEX_INACTIVE = 0;
    public static final int INDEX_RECORDING = 1;

    public RecordAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        Drawable recordInactive = ContextCompat.getDrawable(context, R.drawable.ic_record);
        Drawable recordActive = ContextCompat.getDrawable(context, R.drawable.ic_record_red);
        setDrawables(new Drawable[]{recordInactive, recordActive});
    }
}
