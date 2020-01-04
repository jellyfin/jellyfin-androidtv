package org.jellyfin.androidtv.playback.overlay.actions;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.jellyfin.androidtv.R;

public class SelectAudioAction extends CustomAction {

    public SelectAudioAction(Context context) {
        super(context);
        Drawable drawable = context.getDrawable(R.drawable.ic_select_audio);
        setIcon(drawable);
        setDrawables(new Drawable[]{drawable});
    }


}
