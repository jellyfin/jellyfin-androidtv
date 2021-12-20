package org.jellyfin.androidtv.ui.playback

class VideoSpeedController(playbackController: PlaybackController) {
	companion object {
		enum class SpeedSteps(val value: Double) {
			SPEED_0_25(0.25),
			SPEED_0_50(0.5),
			SPEED_0_75(0.75),
			SPEED_1_00(1.0),
			SPEED_1_25(1.25),
			SPEED_1_50(1.50),
			SPEED_1_75(1.75),
			SPEED_2_00(2.0),
		}
		private var previousSpeedSelection = SpeedSteps.SPEED_1_00
		fun resetPreviousSpeedToDefault(){
			previousSpeedSelection = SpeedSteps.SPEED_1_00
		}
	}

	private val parentController = playbackController
	init {
		// Carry forward the user's recent speed selection onto the next video(s)
		setNewSpeed(previousSpeedSelection)
	}

	fun getCurrentSpeed(): SpeedSteps{
		// Currently getCurrentSpeed uses previousSpeedSelection (from the companion)
		// but this is an implementation detail I'd rather not leak in-case we ever need
		// to separate out the two details. So implement a custom named getter...
		return previousSpeedSelection
	}

	fun setNewSpeed(selectedSpeed: SpeedSteps) {
		previousSpeedSelection = selectedSpeed
		parentController.setPlaybackSpeed(selectedSpeed.value)
	}
}
