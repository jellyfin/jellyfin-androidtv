package org.jellyfin.androidtv.playback.overlay.actions;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.leanback.widget.Action;
import androidx.leanback.widget.PlaybackControlsRow;

import org.jellyfin.androidtv.R;

public class SelectAudioAction extends CustomAction {

    public SelectAudioAction(Context context) {
        super(context);
    }

    @Override
    public Drawable getDrawable(int index) {
        return context.getDrawable(R.drawable.ic_select_audio);
    }

}
