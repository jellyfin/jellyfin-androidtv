package org.jellyfin.androidtv.ui.playback.overlay

import androidx.leanback.widget.PlaybackSeekDataProvider
import kotlin.math.ceil
import kotlin.math.min

class CustomSeekProvider(
	private val videoPlayerAdapter: VideoPlayerAdapter,
) : PlaybackSeekDataProvider() {
	companion object {
		private const val SEEK_LENGTH = 30000L
	}

	override fun getSeekPositions(): LongArray {
		if (!videoPlayerAdapter.canSeek()) return LongArray(0)

		val duration = videoPlayerAdapter.duration
		val size = ceil(duration.toDouble() / SEEK_LENGTH.toDouble()).toInt() + 1
		return LongArray(size) { i -> min(i * SEEK_LENGTH, duration) }
	}
}
