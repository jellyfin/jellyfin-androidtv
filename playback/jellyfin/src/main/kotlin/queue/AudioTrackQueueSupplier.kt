package org.jellyfin.playback.jellyfin.queue

import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.supplier.PagedQueueSupplier
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

class AudioTrackQueueSupplier(
	private val item: BaseItemDto,
	private val api: ApiClient,
) : PagedQueueSupplier() {
	init {
		require(item.type == BaseItemKind.AUDIO)
	}

	override var size: Int = 1

	override suspend fun loadPage(offset: Int, size: Int): Collection<QueueEntry> {
		// We only have a single item
		if (offset > 0) return emptyList()

		val item by api.libraryApi.getItem(itemId = item.id)
		return listOf(createBaseItemQueueEntry(api, item))
	}
}
