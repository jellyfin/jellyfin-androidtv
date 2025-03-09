package org.jellyfin.androidtv.ui.playback.stillwatching

// minMinutes needs to be changed back to Int after testing and 2 test conditions need to be removed
data class StillWatchingStates(val enabled: Boolean, val episodeCount: Int, val minMinutes: Number) {
	companion object {
		fun getSetting(type: String): StillWatchingStates {
			return when (type.lowercase()) {
				"test_episode_count" -> StillWatchingStates(enabled = true, episodeCount = 2, minMinutes = 1)
				"test_min_minutes" -> StillWatchingStates(enabled = true, episodeCount = 5, minMinutes = 0.5)
				"short" -> StillWatchingStates(enabled = true, episodeCount = 2, minMinutes = 60)
				"default" -> StillWatchingStates(enabled = true, episodeCount = 3, minMinutes = 90)
				"long" -> StillWatchingStates(enabled = true, episodeCount = 5, minMinutes = 150)
				"very_long" -> StillWatchingStates(enabled = true, episodeCount = 8, minMinutes = 240)
				"disabled" -> StillWatchingStates(enabled = false, episodeCount = 0, minMinutes = 0)
				else -> StillWatchingStates(enabled = false, episodeCount = 0, minMinutes = 0)
			}
		}
	}
}
