package org.jellyfin.androidtv.playback.nextup

import android.graphics.Bitmap

data class UpNextItemData(
	val id: String,
	val title: String,
	val description: String?,
	val backdrop: Bitmap?,
	val thumbnail: Bitmap?
)
