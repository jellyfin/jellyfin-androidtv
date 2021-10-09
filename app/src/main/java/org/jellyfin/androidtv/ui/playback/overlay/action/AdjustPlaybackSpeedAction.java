package org.jellyfin.androidtv.ui.playback.overlay.action;

import android.content.Context;
import android.view.View;
import android.widget.PopupWindow;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.AudioDelayPopup;
import org.jellyfin.androidtv.ui.PlaybackSpeedPopup;
import org.jellyfin.androidtv.ui.ValueChangedListener;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment;

public class AdjustPlaybackSpeedAction extends CustomAction {

    public AdjustPlaybackSpeedAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        initializeWithIcon(R.drawable.exo_ic_speed);
    }

    @Override
    public void handleClickAction(PlaybackController playbackController, LeanbackOverlayFragment leanbackOverlayFragment, Context context, View view) {
        PlaybackSpeedPopup audioDelayPopup = new PlaybackSpeedPopup(context, view, new ValueChangedListener<Float>() {
            @Override
            public void onValueChanged(Float value) {
                playbackController.setPlaybackSpeed(value);
            }
        });

        PopupWindow popupWindow = audioDelayPopup.getPopupWindow();
        if (popupWindow != null) {
            popupWindow.setOnDismissListener(() -> {
                leanbackOverlayFragment.setFading(true);
            });
        }

        audioDelayPopup.show(playbackController.getPlaybackSpeed());
    }
}
