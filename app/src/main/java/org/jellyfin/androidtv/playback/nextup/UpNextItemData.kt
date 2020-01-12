package org.jellyfin.androidtv.playback.nextup

data class UpNextItemData(
	val id: String,
	val title: String,
	val description: String?,
	val backdrop: String?,
	val thumbnail: String?
)
