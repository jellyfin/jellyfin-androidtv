package org.jellyfin.androidtv.model.itemtypes

import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemPerson
import org.jellyfin.apiclient.model.dto.BaseItemType
import org.jellyfin.apiclient.model.dto.ChapterInfoDto
import org.jellyfin.apiclient.model.entities.ImageType
import org.jellyfin.apiclient.model.entities.PersonType
import java.util.*
import kotlin.properties.Delegates

sealed class BaseItem(original: BaseItemDto) : ObservableParent() {
	val id: String = original.id
	val name: String = original.name
	val description: String? = original.overview
	val images: ImageCollection = ImageCollection(original)
	val added: Date = original.dateCreated
	var favorite: Boolean by Delegates.observable(original.userData.isFavorite, ::observer)
}

sealed class PlayableItem(original: BaseItemDto) : BaseItem(original) {
	var playbackPositionTicks: Long by Delegates.observable(original.userData.playbackPositionTicks, ::observer)
	val mediaInfo = MediaInfo(original.mediaSources, original.mediaStreams)
	val chapters: List<ChapterInfoDto> = original.chapters
	var played: Boolean by Delegates.observable(original.userData.played, ::observer)

	val canResume: Boolean
		get() = playbackPositionTicks > 0L
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

class Person(original: BaseItemPerson) {
	val id: String = original.id
	val name: String = original.name
	val role: String = original.role
	val type: PersonType = original.personType
	val primaryImage: ImageCollection.Image? = original.primaryImageTag?.let { ImageCollection.Image(original.id, ImageType.Primary, it) }
}

class Movie(original: BaseItemDto) : PlayableItem(original) {
	val cast: List<Person> = original.people.asList().map { person -> Person(person) }
}
