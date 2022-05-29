package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.SettingsPopup
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment

class SettingAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue
) : CustomAction(context, customPlaybackTransportControlGlue) {
	var subtitlesPresent = false

	init {
		initializeWithIcon(R.drawable.ic_adjust)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		leanbackOverlayFragment: LeanbackOverlayFragment,
		context: Context,
		view: View
	) = SettingsPopup(context, view,
		{ value -> playbackController.audioDelay = value },
		{ value -> playbackController.subtitleDelay = value }
	).apply {
		popupWindow.setOnDismissListener { leanbackOverlayFragment.setFading(true) }
	}.show(
		subtitlesPresent,
		playbackController.audioDelay,
		playbackController.subtitleDelay
	)
}
