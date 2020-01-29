package org.jellyfin.androidtv.model.itemtypes

import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType
import org.jellyfin.apiclient.model.dto.ChapterInfoDto
import java.util.*

sealed class BaseItem(original: BaseItemDto) {
	val id: String = original.id
	val name: String = original.name
	val description: String? = original.overview
	val images: ImageCollection = ImageCollection(original)
	val added: Date = original.dateCreated
}

sealed class PlayableItem(original: BaseItemDto) : BaseItem(original) {
	val mediaInfo = MediaInfo(original.mediaSources, original.mediaStreams)
	val chapters: List<ChapterInfoDto> = original.chapters
	val canResume: Boolean
		get() = playbackPositionTicks > 0
	var playbackPositionTicks: Long = original.userData.playbackPositionTicks
	var played: Boolean = original.userData.played
}

class Episode(original: BaseItemDto) : PlayableItem(original) {
	val communityRating: Double = original.communityRating.toDouble()

	init {
		if (original.baseItemType != BaseItemType.Episode) {
			throw IllegalArgumentException("Tried to create an Episode from a non-Episode BaseItemDto")
		}
	}
}

class Movie(original: BaseItemDto) : PlayableItem(original)
