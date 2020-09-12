package org.jellyfin.androidtv.ui.playback.overlay.action;

import android.content.Context;
import android.view.View;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment;
import org.jellyfin.androidtv.ui.AudioDelayPopup;
import org.jellyfin.androidtv.ui.ValueChangedListener;

public class AdjustAudioDelayAction extends CustomAction {

    public AdjustAudioDelayAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        initializeWithIcon(R.drawable.ic_adjust);
    }

    @Override
    public void handleClickAction(PlaybackController playbackController, LeanbackOverlayFragment leanbackOverlayFragment, Context context, View view) {
        new AudioDelayPopup(context, view, new ValueChangedListener<Long>() {
            @Override
            public void onValueChanged(Long value) {
                playbackController.setAudioDelay(value);
            }
        }).show(playbackController.getAudioDelay());
    }
}
