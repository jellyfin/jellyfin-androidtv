package org.jellyfin.playback.core.timedevent

import kotlin.time.Duration

/**
 * An event on the playback timeline.
 */
sealed interface TimedEvent {
	/**
	 * Optional key for identifying this timed event.
	 */
	val key: String?

	/**
	 * An instantanious event activated when the player naturally progresses over the [position]. Invokes [onActivate] callback.
	 * Manual seeks over this event will never invoke the callback.
	 */
	data class Instant(
		override val key: String? = null,
		val position: Duration,

		val onActivate: () -> Unit,
	) : TimedEvent

	/**
	 * A block event with a [start] and [end] position. The [onActivate] and [onDeactivate] callbacks will be invoked with [BlockActivation]
	 * metadata.
	 */
	data class Block(
		override val key: String? = null,
		val start: Duration,
		val end: Duration,

		val onActivate: ((metadata: BlockActivation) -> Unit)? = null,
		val onDeactivate: ((metadata: BlockActivation) -> Unit)? = null,
	) : TimedEvent
}
