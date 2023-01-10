package org.jellyfin.playback.core.model

import kotlin.time.Duration

data class PositionInfo(
	/**
	 * The current position. Cannot exceed [duration] unless [duration] is unknown.
	 */
	val active: Duration,

	/**
	 * The buffered position. Cannot exceed [duration] unless [duration] is unknown.
	 */
	val buffer: Duration,

	/**
	 * The maximum position.
	 */
	val duration: Duration,
) {
	companion object {
		val EMPTY = PositionInfo(Duration.ZERO, Duration.ZERO, Duration.ZERO)
	}
}
