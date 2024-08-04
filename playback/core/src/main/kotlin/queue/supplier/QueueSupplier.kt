package org.jellyfin.playback.core.queue.supplier

import org.jellyfin.playback.core.queue.QueueEntry

/**
 * A queue contains all items in the current playback session. This includes already played items,
 * the currently playing item and future items.
 */
interface QueueSupplier {
	/**
	 * The total size of the queue.
	 */
	val size: Int

	suspend fun getItem(index: Int): QueueEntry?
}
