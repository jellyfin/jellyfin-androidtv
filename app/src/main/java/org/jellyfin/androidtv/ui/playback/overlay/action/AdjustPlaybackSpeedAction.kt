package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment
import org.jellyfin.androidtv.ui.PlaybackSpeedPopup
import org.jellyfin.androidtv.ui.ValueChangedListener
import org.jellyfin.androidtv.R

class AdjustPlaybackSpeedAction(
    context: Context?,
    customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue?
) : CustomAction(context, customPlaybackTransportControlGlue) {
    override fun handleClickAction(
        playbackController: PlaybackController,
        leanbackOverlayFragment: LeanbackOverlayFragment,
        context: Context,
        view: View
    ) {
        val audioDelayPopup =
            PlaybackSpeedPopup(context, view, object : ValueChangedListener<Float>() {
                override fun onValueChanged(value: Float) {
                    playbackController.playbackSpeed = value
                }
            })
        val popupWindow = audioDelayPopup.popupWindow
        popupWindow?.setOnDismissListener { leanbackOverlayFragment.setFading(true) }
        audioDelayPopup.show(playbackController.playbackSpeed)
    }

    init {
        initializeWithIcon(R.drawable.exo_ic_speed)
    }
}
