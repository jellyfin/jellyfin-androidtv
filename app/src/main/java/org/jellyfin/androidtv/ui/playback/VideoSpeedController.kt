package org.jellyfin.androidtv.ui.playback

class VideoSpeedController(playbackController: PlaybackController) {
	companion object {
		val speedSteps = doubleArrayOf(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0)
		var mostRecentSpeed = 1.0
	}

	private val parentController = playbackController

	init {
		parentController.setPlaybackSpeed(mostRecentSpeed)
	}

	fun setNewSpeed(speed: Double) {
		mostRecentSpeed = speed
		parentController.setPlaybackSpeed(speed)
	}
}
