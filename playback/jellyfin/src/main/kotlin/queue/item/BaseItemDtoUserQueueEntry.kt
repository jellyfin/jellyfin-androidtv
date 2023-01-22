package org.jellyfin.playback.jellyfin.queue.item

import org.jellyfin.playback.core.queue.item.QueueEntry
import org.jellyfin.playback.core.queue.item.QueueEntryMetadata
import org.jellyfin.playback.core.queue.item.UserQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.extensions.ticks
import java.time.Year
import java.util.UUID

class BaseItemDtoUserQueueEntry private constructor(
	val baseItem: BaseItemDto,
	override val metadata: QueueEntryMetadata,
	base: QueueEntry,
) : QueueEntry by base {
	companion object {
		fun build(api: ApiClient, baseItem: BaseItemDto): BaseItemDtoUserQueueEntry {
			// Create metadata
			val metadata = QueueEntryMetadata(
				mediaId = baseItem.id.toString(),
				title = baseItem.name,
				artist = baseItem.albumArtist,
				album = baseItem.album,
				date = baseItem.dateCreated,
				displayDescription = baseItem.overview,
				displayTitle = baseItem.name,
				duration = baseItem.runTimeTicks?.ticks,
				genre = baseItem.genres?.joinToString(", "),
				year = baseItem.productionYear?.let { Year.of(it) },
				artUri = api.getPrimaryImageUrl(
					itemId = baseItem.id,
					tag = baseItem.imageTags?.get(ImageType.PRIMARY)
				),
				albumArtUri = api.getPrimaryImageUrl(
					itemId = baseItem.albumId ?: baseItem.id,
					tag = baseItem.albumPrimaryImageTag,
				)
			)

			// Return entry
			return BaseItemDtoUserQueueEntry(
				baseItem = baseItem,
				metadata = metadata,
				base = UserQueueEntry()
			)
		}

		/**
		 * Helper extension function to get the URL of the primary image by item id and tag.
		 */
		private fun ApiClient.getPrimaryImageUrl(itemId: UUID?, tag: String?): String? = when {
			// Invalid item id / tag
			itemId == null || tag == null -> null
			// Valid item id & tag
			else -> imageApi.getItemImageUrl(
				itemId = itemId,
				imageType = ImageType.PRIMARY,
				tag = tag,
			)
		}
	}
}
