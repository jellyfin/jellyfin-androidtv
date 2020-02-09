package org.jellyfin.androidtv.model.itemtypes

import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType
import org.jellyfin.apiclient.model.dto.ChapterInfoDto
import org.jellyfin.apiclient.model.dto.GenreDto
import org.jellyfin.apiclient.model.querying.ItemFields
import java.util.*
import kotlin.properties.Delegates

val FIELDS_REQUIRED_FOR_LIFT = arrayOf(ItemFields.DateCreated, ItemFields.MediaSources, ItemFields.MediaStreams, ItemFields.People)

sealed class BaseItem(original: BaseItemDto) : ObservableParent() {
	val id: String = original.id
	val title: String = original.name
	val titleOriginal: String? = original.originalTitle
	val description: String? = original.overview
	val images: ImageCollection = ImageCollection(original)
	val added: Date = original.dateCreated
	var favorite: Boolean by Delegates.observable(original.userData.isFavorite, ::observer)
}

sealed class PlayableItem(original: BaseItemDto) : BaseItem(original) {
	var playbackPositionTicks: Long by Delegates.observable(original.userData.playbackPositionTicks, ::observer)
	val mediaInfo = MediaInfo(original.mediaSources, original.mediaStreams)
	val chapters: List<ChapterInfo> = original.chapters.orEmpty().mapIndexed {
		idx, chapterInfoDto -> ChapterInfo(chapterInfoDto, this, idx)
	}
	var played: Boolean by Delegates.observable(original.userData.played, ::observer)
	val genres: List<GenreDto> = original.genreItems.toList()
	val tags: List<String> = original.tags

	val canResume: Boolean
		get() = playbackPositionTicks > 0L
}

class Episode(original: BaseItemDto) : PlayableItem(original) {
	//TODO: Chapters: ArrayList<ChapterInfoDto>
	val productionYear: Int = original.productionYear
	val communityRating: Double = original.communityRating.toDouble()

	init {
		if (original.baseItemType != BaseItemType.Episode) {
			throw IllegalArgumentException("Tried to create an Episode from a non-Episode BaseItemDto")
		}
	}
}

class Movie(original: BaseItemDto) : PlayableItem(original) {
	var productionYear: Int = original.productionYear
	val cast: List<BriefPersonData> = original.people.asList().map { person -> BriefPersonData(person) }
	val officialRating: String? = original.officialRating
	val communityRating: Float = original.communityRating
	val criticsRating: Float? = original.criticRating
}

class Video(original: BaseItemDto) : PlayableItem(original) {

}
