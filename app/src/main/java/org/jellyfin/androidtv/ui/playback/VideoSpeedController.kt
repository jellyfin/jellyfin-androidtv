package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.R

class VideoSpeedController(
	private val parentController: PlaybackController
) {
	enum class SpeedSteps(val speed: Float, val icon: Int) {
		// Use named parameter so detekt knows these aren't magic values
		SPEED_0_25(speed = 0.25f, icon = R.drawable.ic_playback_speed_0_25),
		SPEED_0_50(speed = 0.5f, icon = R.drawable.ic_playback_speed_0_5),
		SPEED_0_75(speed = 0.75f, icon = R.drawable.ic_playback_speed_0_75),
		SPEED_1_00(speed = 1.0f, icon = R.drawable.ic_playback_speed_1_0),
		SPEED_1_25(speed = 1.25f, icon = R.drawable.ic_playback_speed_1_25),
		SPEED_1_50(speed = 1.50f, icon = R.drawable.ic_playback_speed_1_5),
		SPEED_1_75(speed = 1.75f, icon = R.drawable.ic_playback_speed_1_75),
		SPEED_2_00(speed = 2.0f, icon = R.drawable.ic_playback_speed_2_0),
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

	fun getEnumBySpeed(speed: Float, defaultSpeed: SpeedSteps = SpeedSteps.SPEED_1_00): SpeedSteps {
		val currentSpeed = ((speed * 1000).toInt() + 1) / 10
		for (speedStep in SpeedSteps.entries) {
			if (((speedStep.speed * 1000).toInt() + 1)/10 == currentSpeed) {
				return speedStep
			}
		}
		return defaultSpeed;
	}
}
