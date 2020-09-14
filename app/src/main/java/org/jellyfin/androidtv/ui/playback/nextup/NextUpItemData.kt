package org.jellyfin.androidtv.ui.playback.nextup

import android.graphics.Bitmap

data class NextUpItemData(
	val id: String,
	val title: String,
	val description: String?,
	val backdrop: Bitmap?,
	val thumbnail: Bitmap?,
	val logo: Bitmap?
)
