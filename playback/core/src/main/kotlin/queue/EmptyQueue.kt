package org.jellyfin.playback.core.queue

import org.jellyfin.playback.core.queue.item.QueueEntry

object EmptyQueue : Queue {
	override suspend fun getItem(index: Int): QueueEntry? = null
}
