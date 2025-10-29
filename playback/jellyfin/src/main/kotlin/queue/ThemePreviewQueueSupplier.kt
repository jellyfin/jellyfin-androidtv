package org.jellyfin.playback.jellyfin.queue

import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.supplier.PagedQueueSupplier
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

/**
 * Queue supplier for a single AUDIO item (the show's theme track).
 */
class ThemePreviewQueueSupplier(
	private val item: BaseItemDto,
	private val api: ApiClient,
) : PagedQueueSupplier() {

	init {
		require(item.type == BaseItemKind.AUDIO)
	}

	override var size: Int = 1

	override suspend fun loadPage(
		offset: Int,
		size: Int,
	): Collection<QueueEntry> {
		// We only have one entry. Anything after index 0 returns nothing.
		if (offset > 0) return emptyList()

		val fullItem by api.userLibraryApi.getItem(itemId = item.id)

		return listOf(
			createBaseItemQueueEntry(
				api = api,
				baseItem = fullItem,
			)
		)
	}
}
