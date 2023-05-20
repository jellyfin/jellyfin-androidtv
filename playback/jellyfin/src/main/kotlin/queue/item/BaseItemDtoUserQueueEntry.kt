package org.jellyfin.playback.jellyfin.queue.item

import org.jellyfin.playback.core.queue.item.QueueEntry
import org.jellyfin.playback.core.queue.item.QueueEntryMetadata
import org.jellyfin.playback.core.queue.item.UserQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.extensions.ticks
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
				duration = baseItem.runTimeTicks?.ticks,
				title = baseItem.name,
				artist = baseItem.albumArtist,
				albumTitle = baseItem.album,
				albumArtist = baseItem.albumArtists
					?.mapNotNull { it.name }
					?.joinToString(", "),
				displayTitle = baseItem.name,
				description = baseItem.overview,
				artworkUri = when {
					baseItem.imageTags?.containsKey(ImageType.PRIMARY) == true -> api.getImageUri(
						itemId = baseItem.id,
						tag = baseItem.imageTags!![ImageType.PRIMARY]
					)

					baseItem.albumPrimaryImageTag != null -> api.getImageUri(
						itemId = baseItem.albumId ?: baseItem.id,
						tag = baseItem.albumPrimaryImageTag,
					)

					else -> null
				},
				trackNumber = baseItem.indexNumber,
				releaseDate = baseItem.premiereDate?.toLocalDate(),
				genre = baseItem.genres?.joinToString(", "),
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
		private fun ApiClient.getImageUri(itemId: UUID?, tag: String?): String? = when {
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
