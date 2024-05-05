package org.jellyfin.playback.core.queue

data object EmptyQueue : Queue {
	override val size: Int = 0
	override suspend fun getItem(index: Int): QueueEntry? = null
}
