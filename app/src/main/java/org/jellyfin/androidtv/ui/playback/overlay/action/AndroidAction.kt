package org.jellyfin.androidtv.ui.playback.overlay.action

import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

interface AndroidAction {
	fun onActionClicked(
		videoPlayerAdapter: VideoPlayerAdapter
	)
}
