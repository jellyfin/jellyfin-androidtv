package org.jellyfin.playback.core.queue

import org.jellyfin.playback.core.queue.item.QueueEntry

class QueueManager {
	companion object {
		const val POSITION_NONE = -1
	}

	private var currentQueue: Queue? = null

	private var currentItem: QueueEntry? = null
	private var currentItemPosition = POSITION_NONE

	fun updateQueue(queue: Queue) {
		currentItem = null
		currentItemPosition = POSITION_NONE
		currentQueue = queue
	}

	suspend fun jumpTo(index: Int): QueueEntry? {
		require(index >= 0)

		currentItemPosition = index
		currentItem = currentQueue?.getItem(currentItemPosition)
		if (currentItem == null) currentItemPosition = POSITION_NONE

		return currentItem
	}

	suspend fun next(): QueueEntry? = jumpTo(currentItemPosition + 1)
	suspend fun peekNext(): QueueEntry? = currentQueue?.getItem(currentItemPosition + 1)
	suspend fun peek(amount: Int): Collection<QueueEntry> = Array(amount) { i ->
		currentQueue?.getItem(currentItemPosition + i + 1)
	}.filterNotNull()

	suspend fun previous(): QueueEntry? = jumpTo(currentItemPosition - 1)
}
