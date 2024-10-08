package org.jellyfin.playback.jellyfin.queue

import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.supplier.PagedQueueSupplier
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.instantMixApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields

class AudioInstantMixQueueSupplier(
	private val item: BaseItemDto,
	private val api: ApiClient,
) : PagedQueueSupplier() {
	companion object {
		val instantMixableItems = arrayOf(
			BaseItemKind.MUSIC_GENRE,
			BaseItemKind.PLAYLIST,
			BaseItemKind.MUSIC_ALBUM,
			BaseItemKind.MUSIC_ARTIST,
			BaseItemKind.AUDIO,
			BaseItemKind.FOLDER,
		)
	}

	init {
		require(item.type in instantMixableItems)
	}

	override var size: Int = 0
		private set

	override suspend fun loadPage(offset: Int, size: Int): Collection<QueueEntry> {
		// API doesn't support paging for instant mix
		if (offset > 0) return emptyList()

		val result by api.instantMixApi.getInstantMixFromItem(
			itemId = item.id,
			fields = listOf(ItemFields.MEDIA_SOURCES),
			// Pagination
			limit = size,
		)
		this.size = result.totalRecordCount
		return result.items.map { createBaseItemQueueEntry(api, it) }
	}
}
