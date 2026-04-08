package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.PlaybackInfoPopup
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class PlaybackInfoAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var popup: PlaybackInfoPopup? = null

	init {
		initializeWithIcon(R.drawable.ic_info)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		dismissPopup()

		popup = PlaybackInfoPopup(context, playbackController).apply {
			setOnDismissListener {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				popup = null
			}
			show(view)
		}
	}

	fun dismissPopup() {
		popup?.dismiss()
	}
}
