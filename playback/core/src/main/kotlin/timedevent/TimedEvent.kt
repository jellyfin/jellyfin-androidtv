package org.jellyfin.playback.core.timedevent

import kotlin.time.Duration

sealed interface TimedEvent {
	/**
	 * Optional key for identifying this timed event.
	 */
	val key: String?

	/**
	 * The position of this timed event.
	 */
	val position: Duration

	/**
	 * Function callback variant of a timed event.
	 */
	data class Callback(
		override val key: String? = null,
		override val position: Duration,

		val callback: () -> Unit,
	) : TimedEvent
}
