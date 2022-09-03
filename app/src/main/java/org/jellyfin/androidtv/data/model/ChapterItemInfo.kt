package org.jellyfin.androidtv.data.model

import java.util.UUID

data class ChapterItemInfo(
	val itemId: UUID,
	val name: String?,
	val startPositionTicks: Long,
	val imagePath: String?,
)
