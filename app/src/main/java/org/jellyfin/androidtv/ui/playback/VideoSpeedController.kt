package org.jellyfin.androidtv.ui.playback

class VideoSpeedController(
	private val parentController: PlaybackController
) {
	enum class SpeedSteps(val speed: Double) {
		// Use named parameter so detekt knows these aren't magic values
		SPEED_0_25(speed = 0.25),
		SPEED_0_50(speed = 0.5),
		SPEED_0_75(speed = 0.75),
		SPEED_1_00(speed = 1.0),
		SPEED_1_25(speed = 1.25),
		SPEED_1_50(speed = 1.50),
		SPEED_1_75(speed = 1.75),
		SPEED_2_00(speed = 2.0),
	}

	companion object {
		// Preserve the currently selected speed during the app lifetime, even if
		// video playback closes
		private var previousSpeedSelection = SpeedSteps.SPEED_1_00
	}

	var currentSpeed = previousSpeedSelection
		set(value) {
			val checkedVal = if (parentController.isLiveTv) SpeedSteps.SPEED_1_00 else value
			parentController.setPlaybackSpeed(checkedVal.speed)

			previousSpeedSelection = checkedVal
			field = checkedVal
		}

	init {
		// We need to do this again in init, as Kotlin will not call the custom
		// setter on initialization, so the PlaybackController is not informed
		currentSpeed = previousSpeedSelection
	}

	fun resetSpeedToDefault() {
		currentSpeed = SpeedSteps.SPEED_1_00
	}
}
