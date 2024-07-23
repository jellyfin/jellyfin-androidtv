package org.jellyfin.androidtv.ui.playback.overlay

import androidx.leanback.widget.PlaybackSeekDataProvider
import org.jellyfin.androidtv.preference.UserPreferences
import kotlin.math.ceil
import kotlin.math.min

class CustomSeekProvider(
	private val videoPlayerAdapter: VideoPlayerAdapter,
	private val userPreferences: UserPreferences,
) : PlaybackSeekDataProvider() {
	override fun getSeekPositions(): LongArray {
		if (!videoPlayerAdapter.canSeek()) return LongArray(0)

		val seekTime = userPreferences[UserPreferences.seekTime];
		val duration = videoPlayerAdapter.duration

		val size = ceil(duration.toDouble() / seekTime.toDouble()).toInt() + 1
		return LongArray(size) { i -> min(i * seekTime.toLong(), duration) }
	}
}
