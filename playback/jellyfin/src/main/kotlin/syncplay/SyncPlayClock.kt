package org.jellyfin.playback.jellyfin.syncplay

import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Jellyfin's four-timestamp clock estimator. All inputs are milliseconds; monotonic time must
 * include device sleep (elapsedRealtime on Android). Confine access to the coordinator's dispatcher.
 */
class SyncPlayClock {
	private data class Measurement(val offset: Long, val delay: Long)

	private val measurements = ArrayDeque<Measurement>()
	private var anchor: Pair<Long, Long>? = null
	private var measurement: Measurement? = null

	val pingMillis: Long? get() = measurement?.let { (it.delay / 2.0).roundToLong() }

	/** Returns false for invalid measurements, including wall-clock changes during the request. */
	@Suppress("LongParameterList")
	fun record(
		clientSent: Long,
		serverReceived: Long,
		serverSent: Long,
		clientReceived: Long,
		monotonicSent: Long,
		monotonicReceived: Long,
	): Boolean {
		val elapsed = monotonicReceived - monotonicSent
		val wallElapsed = clientReceived - clientSent
		val serverElapsed = serverSent - serverReceived
		if (elapsed < 0 || serverElapsed < 0) return false
		if (abs(wallElapsed.toDouble() - elapsed) > MAX_CLOCK_CHANGE_MILLIS) {
			reset()
			return false
		}
		val previous = anchor
		val anchorDrift = previous?.let { (clientReceived - it.first).toDouble() - (monotonicReceived - it.second) }
		if (anchorDrift != null && abs(anchorDrift) > MAX_CLOCK_CHANGE_MILLIS) {
			reset()
			return false
		}
		val delay = elapsed - serverElapsed
		if (delay < 0) return false
		val offset = ((serverReceived.toDouble() - clientSent + serverSent.toDouble() - clientReceived) / 2).roundToLong()
		measurements.addLast(Measurement(offset, delay))
		if (measurements.size > WINDOW_SIZE) measurements.removeFirst()
		measurement = measurements.minBy { it.delay }
		anchor = clientReceived to monotonicReceived
		return true
	}

	/** Advances the latest wall-clock anchor using monotonic time, never the mutable wall clock. */
	fun serverTime(monotonicNow: Long): Instant? {
		val (wall, monotonic) = anchor ?: return null
		val offset = measurement?.offset ?: return null
		if (monotonicNow < monotonic) return null
		return Instant.ofEpochMilli(wall + offset + (monotonicNow - monotonic))
	}

	fun reset() {
		measurements.clear()
		measurement = null
		anchor = null
	}

	private companion object {
		const val WINDOW_SIZE = 8
		const val MAX_CLOCK_CHANGE_MILLIS = 1000
	}
}
