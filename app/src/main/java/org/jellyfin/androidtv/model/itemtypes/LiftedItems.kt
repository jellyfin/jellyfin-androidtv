package org.jellyfin.androidtv.model.itemtypes

import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType

sealed class BaseItem(original: BaseItemDto) {
	val id: String = original.id
	val name: String = original.name
	val description: String = original.overview
}

sealed class PlayableItem(original: BaseItemDto) : BaseItem(original) {
	val canResume: Boolean = original.canResume
	val playbackPositionTicks: Long = original.userData.playbackPositionTicks
}

class Episode(original: BaseItemDto) : PlayableItem(original) {
	//TODO: Chapters: ArrayList<ChapterInfoDto>
	val communityRating: Double = original.communityRating.toDouble()

	init {
		if (original.baseItemType != BaseItemType.Episode) {
			throw IllegalArgumentException("Tried to create an Episode from a non-Episode BaseItemDto")
		}
	}
}
