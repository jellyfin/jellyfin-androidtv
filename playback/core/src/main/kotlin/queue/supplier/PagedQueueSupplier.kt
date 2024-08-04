package org.jellyfin.playback.core.queue.supplier

import org.jellyfin.playback.core.queue.QueueEntry

abstract class PagedQueueSupplier(
	private val pageSize: Int = 10,
) : QueueSupplier {
	companion object {
		const val MAX_SIZE = 100
	}

	private val buffer: MutableList<QueueEntry> = mutableListOf()

	override suspend fun getItem(index: Int): QueueEntry? {
		require(index in 0 until MAX_SIZE)

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
