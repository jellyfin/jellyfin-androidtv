package org.jellyfin.playback.jellyfin.queue

import org.jellyfin.playback.core.queue.PagedQueue
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

class AudioTrackQueue(
	private val item: BaseItemDto,
	private val api: ApiClient,
) : PagedQueue() {
	init {
		require(item.type == BaseItemKind.AUDIO)
	}

	override var size: Int = 1

	override suspend fun loadPage(offset: Int, size: Int): Collection<QueueEntry> {
		// We only have a single item
		if (offset > 0) return emptyList()

		val item by api.userLibraryApi.getItem(itemId = item.id)
		return listOf(createBaseItemQueueEntry(api, item))
	}
}
