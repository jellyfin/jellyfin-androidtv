package org.jellyfin.androidtv.ui.playback.stillwatching

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

data class StillWatchingStates(val enabled: Boolean, val episodeCount: Int, val minDuration: Duration) {
	companion object {
		fun getSetting(type: StillWatchingPresetConfigs): StillWatchingStates {
			return when (type) {
				StillWatchingPresetConfigs.SHORT -> StillWatchingStates(enabled = true, episodeCount = 2, minDuration = 1.hours)
				StillWatchingPresetConfigs.DEFAULT -> StillWatchingStates(enabled = true, episodeCount = 3, minDuration = 1.5.hours)
				StillWatchingPresetConfigs.LONG -> StillWatchingStates(enabled = true, episodeCount = 5, minDuration = 2.5.hours)
				StillWatchingPresetConfigs.VERY_LONG -> StillWatchingStates(enabled = true, episodeCount = 8, minDuration = 4.hours)
				StillWatchingPresetConfigs.DISABLED -> StillWatchingStates(enabled = false, episodeCount = 0, minDuration = Duration.ZERO)
			}
		}
	}
}
