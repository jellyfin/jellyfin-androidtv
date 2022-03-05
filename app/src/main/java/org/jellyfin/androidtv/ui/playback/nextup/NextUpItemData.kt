package org.jellyfin.androidtv.ui.playback.nextup

import android.graphics.Bitmap
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

data class NextUpItemData(
	val baseItem: BaseItemDto,
	val id: UUID,
	val title: String,
	val thumbnail: Bitmap?,
	val logo: Bitmap?
)
