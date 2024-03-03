package org.jellyfin.playback.core.queue

import org.jellyfin.playback.core.queue.item.QueueEntry

data object EmptyQueue : Queue {
	override val size: Int = 0
	override suspend fun getItem(index: Int): QueueEntry? = null
}
