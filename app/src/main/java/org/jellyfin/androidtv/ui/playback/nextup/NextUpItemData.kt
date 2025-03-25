package org.jellyfin.androidtv.ui.playback.nextup

import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

data class NextUpItemData(
	val baseItem: BaseItemDto,
	val id: UUID,
	val title: String,
	val thumbnail: Image?,
	val logo: Image?,
) {
	data class Image(val url: String, val blurHash: String?, val aspectRatio: Double)
}
