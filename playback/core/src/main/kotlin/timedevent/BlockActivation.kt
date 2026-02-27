package org.jellyfin.playback.core.timedevent

import kotlin.time.Duration

/**
 * Metadata passed as part of a [TimedEvent.Block] activation/deactivation invocation.
 */
sealed interface BlockActivation {
	/**
	 * This block was (de)activated by natural playback progression. The callback was invoked at position [to].
	 */
	data class Natural(
		val to: Duration
	) : BlockActivation

	/**
	 * This block was (de)activated by a seek action. The callback was invoked when seeking from [from] to [to].
	 */
	data class Seek(
		val from: Duration,
		val to: Duration,
	) : BlockActivation
}
