package org.jellyfin.playback.media3.exoplayer.subtitle

import androidx.media3.common.util.UnstableApi
import java.util.concurrent.CopyOnWriteArraySet

@UnstableApi
class SubtitleTimingOffsetState(initialOffsetUs: Long = 0L) {
	fun interface Listener {
		fun onSubtitleTimingOffsetChanged(offsetUs: Long)
	}

	private val listeners = CopyOnWriteArraySet<Listener>()

	@Volatile
	private var currentOffsetUs = initialOffsetUs

	val offsetUs: Long
		get() = currentOffsetUs

	fun setOffsetUs(offsetUs: Long) {
		currentOffsetUs = offsetUs
		listeners.forEach { it.onSubtitleTimingOffsetChanged(offsetUs) }
	}

	fun adjustOffsetUs(deltaUs: Long) {
		setOffsetUs(currentOffsetUs + deltaUs)
	}

	fun addListener(listener: Listener) {
		listeners.add(listener)
		listener.onSubtitleTimingOffsetChanged(currentOffsetUs)
	}

	fun removeListener(listener: Listener) {
		listeners.remove(listener)
	}
}
