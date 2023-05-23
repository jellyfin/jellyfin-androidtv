package org.jellyfin.playback.jellyfin.queue

import org.jellyfin.playback.core.queue.PagedQueue
import org.jellyfin.playback.core.queue.item.QueueEntry
import org.jellyfin.playback.jellyfin.queue.item.BaseItemDtoUserQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.constant.MediaType

class AudioAlbumQueue(
	private val album: BaseItemDto,
	private val api: ApiClient,
) : PagedQueue() {
	init {
		require(album.type == BaseItemKind.MUSIC_ALBUM)
	}

	override var size: Int = 0
		private set

	override suspend fun loadPage(offset: Int, size: Int): Collection<QueueEntry> {
		val result by api.itemsApi.getItemsByUserId(
			parentId = album.id,
			recursive = true,
			mediaTypes = listOf(MediaType.Audio),
			includeItemTypes = listOf(BaseItemKind.AUDIO),
			sortBy = listOf(ItemFields.SORT_NAME.name),
			fields = listOf(ItemFields.MEDIA_SOURCES),
			// Pagination
			startIndex = offset,
			limit = size,
			enableTotalRecordCount = true,
		)
		this.size = result.totalRecordCount
		return result.items.orEmpty().map { BaseItemDtoUserQueueEntry.build(api, it) }
	}
}
