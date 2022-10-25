package org.jellyfin.playback.core.queue

import org.jellyfin.playback.core.queue.item.QueueEntry

/**
 * A queue contains all items in the current playback session. This includes already played items,
 * the currently playing item and future items.
 */
interface Queue {
	// Item retrieval
	suspend fun getItem(index: Int): QueueEntry?
}
