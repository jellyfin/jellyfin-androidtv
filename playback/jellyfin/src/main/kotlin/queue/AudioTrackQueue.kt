package org.jellyfin.playback.jellyfin.queue

import org.jellyfin.playback.core.queue.PagedQueue
import org.jellyfin.playback.core.queue.item.QueueEntry
import org.jellyfin.playback.jellyfin.queue.item.BaseItemDtoUserQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

class AudioTrackQueue(
	private val item: BaseItemDto,
	private val api: ApiClient,
) : PagedQueue() {
	init {
		require(item.type == BaseItemKind.AUDIO)
	}

	override suspend fun loadPage(offset: Int, size: Int): Collection<QueueEntry> {
		// We only have a single item
		if (offset > 0) return emptyList()

		return listOf(BaseItemDtoUserQueueEntry.build(api, item))
	}
}
