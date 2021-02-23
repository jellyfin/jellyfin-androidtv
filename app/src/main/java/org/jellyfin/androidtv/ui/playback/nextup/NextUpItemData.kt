package org.jellyfin.androidtv.ui.playback.nextup

import android.graphics.Bitmap
import org.jellyfin.apiclient.model.dto.BaseItemDto

data class NextUpItemData(
	val baseItem: BaseItemDto,
	val id: String,
	val title: String,
	val description: String?,
	val thumbnail: Bitmap?,
	val logo: Bitmap?
)
