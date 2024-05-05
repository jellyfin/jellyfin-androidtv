package org.jellyfin.playback.core.queue

abstract class PagedQueue(
	private val pageSize: Int = 10,
) : Queue {
	private val buffer: MutableList<QueueEntry> = mutableListOf()

	override suspend fun getItem(index: Int): QueueEntry? {
		require(index in 0 until SequenceQueue.MAX_SIZE)

		var page: Collection<QueueEntry>
		var pageOffset = buffer.size
		do {
			if (buffer.size > index) return buffer[index]
			page = loadPage(pageOffset, pageSize)
			pageOffset += page.size

			for (item in page) buffer.add(item)
		} while (page.isNotEmpty())

		return null
	}

	abstract suspend fun loadPage(offset: Int, size: Int): Collection<QueueEntry>
}
