package org.jellyfin.playback.jellyfin.syncplay

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Jellyfin Web's default SpeedToSync/SkipToSync policy. Ongoing correction is opt-in, as in Web.
 * The caller schedules cooldowns, restores speed after [SyncPlayCorrection.ChangeSpeed], and
 * cancels correction whenever a new group command arrives or the client leaves the group.
 *
 * See Jellyfin Web's `src/plugins/syncPlay/core/PlaybackCore.js`.
 */
class SyncPlayDriftPolicy {
	val correctionIntervalMillis: Long = CORRECTION_INTERVAL_MILLIS
	val unpauseGracePeriodMillis: Long = UNPAUSE_GRACE_PERIOD_MILLIS

	/** Positive drift means the client is behind the group. */
	fun correction(
		driftMillis: Double,
		supportsPlaybackRate: Boolean,
		enabled: Boolean = false,
	): SyncPlayCorrection {
		if (!enabled || !driftMillis.isFinite()) return SyncPlayCorrection.None
		val absoluteDriftMillis = abs(driftMillis)
		if (supportsPlaybackRate && absoluteDriftMillis >= MIN_SPEED_DRIFT_MILLIS && absoluteDriftMillis < MAX_SPEED_DRIFT_MILLIS) {
			var durationMillis = SPEED_DURATION_MILLIS
			// Web lengthens recovery when ahead to avoid a non-positive playback rate.
			if (driftMillis <= -durationMillis * MIN_SPEED) {
				durationMillis = absoluteDriftMillis / (1 - MIN_SPEED)
			}
			return SyncPlayCorrection.ChangeSpeed(
				rate = (1 + driftMillis / durationMillis).toFloat(),
				durationMillis = durationMillis.roundToLong(),
			)
		}
		if (absoluteDriftMillis >= MIN_SEEK_DRIFT_MILLIS) return SyncPlayCorrection.Seek
		return SyncPlayCorrection.None
	}

	private companion object {
		const val CORRECTION_INTERVAL_MILLIS = 1500L
		const val UNPAUSE_GRACE_PERIOD_MILLIS = 1500L
		const val MIN_SPEED_DRIFT_MILLIS = 60.0
		const val MAX_SPEED_DRIFT_MILLIS = 3000.0
		const val SPEED_DURATION_MILLIS = 1000.0
		const val MIN_SEEK_DRIFT_MILLIS = 400.0
		const val MIN_SPEED = 0.2
	}
}

sealed interface SyncPlayCorrection {
	data object None : SyncPlayCorrection
	data class ChangeSpeed(val rate: Float, val durationMillis: Long) : SyncPlayCorrection
	data object Seek : SyncPlayCorrection
}
