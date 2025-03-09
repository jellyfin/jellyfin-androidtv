package org.jellyfin.androidtv.ui.playback.stillwatching

data class StillWatchingStates(val enabled: Boolean, val episodeCount: Int, val minMinutes: Number) {
	companion object {
		fun getSetting(type: StillWatchingPresetConfigs): StillWatchingStates {
			return when (type) {
				StillWatchingPresetConfigs.SHORT -> StillWatchingStates(enabled = true, episodeCount = 2, minMinutes = 60)
				StillWatchingPresetConfigs.DEFAULT -> StillWatchingStates(enabled = true, episodeCount = 3, minMinutes = 90)
				StillWatchingPresetConfigs.LONG -> StillWatchingStates(enabled = true, episodeCount = 5, minMinutes = 150)
				StillWatchingPresetConfigs.VERY_LONG -> StillWatchingStates(enabled = true, episodeCount = 8, minMinutes = 240)
				StillWatchingPresetConfigs.DISABLED -> StillWatchingStates(enabled = false, episodeCount = 0, minMinutes = 0)
			}
		}
	}
}
