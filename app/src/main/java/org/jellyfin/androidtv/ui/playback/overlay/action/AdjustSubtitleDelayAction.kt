package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.SubtitleDelayPopup
import org.jellyfin.androidtv.ui.ValueChangedListener
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment

class AdjustSubtitleDelayAction(
	context: Context?,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue?
) :	CustomAction(context, customPlaybackTransportControlGlue) {
	override fun handleClickAction(
		playbackController: PlaybackController,
		leanbackOverlayFragment: LeanbackOverlayFragment,
		context: Context,
		view: View
	) {

		val subtitleDelayPopup =
			SubtitleDelayPopup(context, view) {
					value ->
					playbackController.subtitleDelay = value
			}

		val popupWindow = subtitleDelayPopup.popupWindow
		popupWindow?.setOnDismissListener {
			leanbackOverlayFragment.setFading(true)
		}

		subtitleDelayPopup.show(playbackController.subtitleDelay)
	}

	init {
		initializeWithIcon(R.drawable.ic_adjust)
	}
}
