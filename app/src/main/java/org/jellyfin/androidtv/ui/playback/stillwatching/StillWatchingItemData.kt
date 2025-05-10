package org.jellyfin.androidtv.ui.playback.stillwatching

import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

data class StillWatchingItemData(
	val baseItem: BaseItemDto,
	val id: UUID,
	val title: String,
	val thumbnail: Image?,
	val logo: Image?,
) {
	data class Image(val url: String, val blurHash: String?, val aspectRatio: Double)
}
