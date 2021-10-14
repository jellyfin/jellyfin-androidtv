package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment
import org.jellyfin.androidtv.ui.PlaybackSpeedPopup
import org.jellyfin.androidtv.R

class AdjustPlaybackSpeedAction(
    context: Context,
    customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue
) : CustomAction(context, customPlaybackTransportControlGlue) {
    override fun handleClickAction(
        playbackController: PlaybackController,
        leanbackOverlayFragment: LeanbackOverlayFragment,
        context: Context,
        view: View
    ) {
        val playbackSpeedPopup = PlaybackSpeedPopup(context, view) { value: Float ->
			playbackController.playbackSpeed = value
		}
		playbackSpeedPopup.popupWindow?.setOnDismissListener { leanbackOverlayFragment.setFading(true) }
        playbackSpeedPopup.show(playbackController.playbackSpeed)
    }

    init {
        initializeWithIcon(R.drawable.exo_ic_speed)
    }
}
