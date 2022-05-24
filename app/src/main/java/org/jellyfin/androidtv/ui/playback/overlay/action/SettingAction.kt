package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.SettingsPopup
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment

class SettingAction(
	context: Context?,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue?
) :	CustomAction(context, customPlaybackTransportControlGlue) {
	private var subtitlesPresent = false

	override fun handleClickAction(
		playbackController: PlaybackController,
		leanbackOverlayFragment: LeanbackOverlayFragment,
		context: Context,
		view: View
	) {


		val settingsPopup =
			SettingsPopup(context, view,
				{ value -> playbackController.audioDelay = value },
				{ value -> playbackController.subtitleDelay = value }
			)

		val popupWindow = settingsPopup.popupWindow
		popupWindow?.setOnDismissListener {
			leanbackOverlayFragment.setFading(true)
		}

		settingsPopup.setSubtitlesPresent(context, subtitlesPresent)
		settingsPopup.show(playbackController.audioDelay, playbackController.subtitleDelay)
	}

	init {
		initializeWithIcon(R.drawable.ic_adjust)
	}

	fun setSubtitlesPresent(value: Boolean) {
		subtitlesPresent = value
	}

}
