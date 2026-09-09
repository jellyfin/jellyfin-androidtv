package org.jellyfin.playback.jellyfin.syncplay

import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

/**
 * Estimates server UTC using Jellyfin Web's lowest-delay sample from the last eight measurements.
 * Elapsed time comes from a monotonic clock so changes to the device clock cannot move playback.
 *
 * See Jellyfin Web's `src/plugins/syncPlay/core/timeSync/TimeSync.js`.
 */
class SyncPlayClock(
	private val wallTimeMillis: () -> Long = System::currentTimeMillis,
	private val monotonicTimeNanos: () -> Long = System::nanoTime,
) {
	private var anchorNanos = monotonicTimeNanos()
	private var anchorEpochMillis = wallTimeMillis()
	private var generation = 0L
	private val measurements = ArrayDeque<Measurement>()
	private var selectedMeasurement: Measurement? = null

	val isSynchronized: Boolean
		get() = synchronized(this) { selectedMeasurement != null }

	/** Server UTC minus the monotonic projection of the device's initial UTC, in milliseconds. */
	val offsetMillis: Double
		get() = synchronized(this) { selectedMeasurement?.offsetMillis ?: 0.0 }

	/** Jellyfin's Ping request expects one-way latency, rounded from half the network round trip. */
	val pingMillis: Long
		get() = synchronized(this) { ((selectedMeasurement?.delayMillis ?: 0.0) / 2).roundToLong() }

	@Synchronized
	fun beginMeasurement(): MeasurementRequest {
		val sentAtNanos = monotonicTimeNanos()
		return MeasurementRequest(generation, sentAtNanos, localTimeMillis(sentAtNanos))
	}

	/** Returns false for stale requests or timestamps that cannot represent a valid measurement. */
	@Synchronized
	fun completeMeasurement(
		request: MeasurementRequest,
		serverReceivedMillis: Long,
		serverSentMillis: Long,
	): Boolean {
		if (request.generation != generation || serverSentMillis < serverReceivedMillis) return false
		val receivedAtNanos = monotonicTimeNanos()
		val elapsedNanos = receivedAtNanos - request.sentAtNanos
		if (elapsedNanos < 0) return false

		val elapsedMillis = elapsedNanos / NANOS_PER_MILLISECOND
		val serverProcessingMillis = serverSentMillis.toDouble() - serverReceivedMillis.toDouble()
		val delayMillis = elapsedMillis - serverProcessingMillis
		if (delayMillis < 0) return false

		val responseReceivedMillis = request.localSentMillis + elapsedMillis
		val offsetMillis = ((serverReceivedMillis - request.localSentMillis) + (serverSentMillis - responseReceivedMillis)) / 2
		measurements.addLast(Measurement(offsetMillis, delayMillis))
		if (measurements.size > TRACKED_MEASUREMENTS) measurements.removeFirst()
		selectedMeasurement = measurements.minBy { it.delayMillis }
		return true
	}

	/** Current estimated server UTC, suitable for Ready/Buffering request timestamps. */
	@Synchronized
	fun nowMillis(): Long = (localTimeMillis(monotonicTimeNanos()) + offsetMillis).roundToLong()

	/** Delay for a command's server UTC timestamp; late commands should run immediately. */
	@Synchronized
	fun delayUntil(serverTimeMillis: Long): Long = (serverTimeMillis - nowMillis()).coerceAtLeast(0)

	/** Monotonic elapsed milliseconds for correction cooldowns and player event deadlines. */
	@Synchronized
	fun monotonicMillis(): Long = TimeUnit.NANOSECONDS.toMillis(monotonicTimeNanos() - anchorNanos)

	/** Discards measurements and invalidates requests still in flight when changing servers. */
	@Synchronized
	fun reset() {
		generation++
		measurements.clear()
		selectedMeasurement = null
		anchorNanos = monotonicTimeNanos()
		anchorEpochMillis = wallTimeMillis()
	}

	private fun localTimeMillis(nanos: Long): Double = anchorEpochMillis + (nanos - anchorNanos) / NANOS_PER_MILLISECOND

	class MeasurementRequest internal constructor(
		internal val generation: Long,
		internal val sentAtNanos: Long,
		internal val localSentMillis: Double,
	)

	private data class Measurement(val offsetMillis: Double, val delayMillis: Double)

	private companion object {
		const val TRACKED_MEASUREMENTS = 8
		const val NANOS_PER_MILLISECOND = 1_000_000.0
	}
}
