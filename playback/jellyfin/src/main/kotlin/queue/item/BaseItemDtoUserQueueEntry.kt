package org.jellyfin.playback.jellyfin.queue.item

import org.jellyfin.playback.core.queue.item.QueueEntryMetadata
import org.jellyfin.playback.core.queue.item.UserQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import kotlin.time.Duration.Companion.nanoseconds

class BaseItemDtoUserQueueEntry private constructor(
	val baseItem: BaseItemDto,
	override val metadata: QueueEntryMetadata,
) : UserQueueEntry() {
	companion object {
		fun build(api: ApiClient, baseItem: BaseItemDto): BaseItemDtoUserQueueEntry {
			val metadata = QueueEntryMetadata(
				mediaId = baseItem.id.toString(),
				title = baseItem.name,
				artist = baseItem.albumArtist,
				album = baseItem.album,
				date = baseItem.dateCreated?.toString(),
				displayDescription = baseItem.overview,
				displayTitle = baseItem.name,
				duration = baseItem.runTimeTicks?.ticks?.inWholeMilliseconds,
				genre = baseItem.genres?.joinToString(", "),
				year = baseItem.productionYear?.toLong(),
				artUri = baseItem.imageTags?.get(ImageType.PRIMARY)?.let { tag ->
					api.imageApi.getItemImageUrl(
						itemId = baseItem.id,
						imageType = ImageType.PRIMARY,
						tag = tag,
					)
				},
				albumArtUri = baseItem.albumPrimaryImageTag?.let { tag ->
					api.imageApi.getItemImageUrl(
						itemId = baseItem.albumId ?: baseItem.id,
						imageType = ImageType.PRIMARY,
						tag = tag,
					)
				}
			)

			return BaseItemDtoUserQueueEntry(baseItem, metadata)
		}

		/**
		 * Convert ticks to duration
		 */
		private val Long.ticks get() = div(100L).nanoseconds
	}
}
