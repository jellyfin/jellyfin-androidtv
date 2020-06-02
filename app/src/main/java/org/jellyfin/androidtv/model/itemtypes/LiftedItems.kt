package org.jellyfin.androidtv.model.itemtypes

import org.jellyfin.androidtv.model.trailers.external.ExternalTrailer
import org.jellyfin.androidtv.model.trailers.lifter.BaseTrailerLifter
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.GenreDto
import org.jellyfin.apiclient.model.dto.NameIdPair
import org.jellyfin.apiclient.model.querying.ItemFields
import java.util.*
import kotlin.properties.Delegates

val FIELDS_REQUIRED_FOR_LIFT = arrayOf(
	ItemFields.DateCreated,
	ItemFields.MediaSources,
	ItemFields.MediaStreams,
	ItemFields.People,
	ItemFields.Genres,
	ItemFields.Tags,
	ItemFields.RemoteTrailers,
	ItemFields.CanDelete,
	ItemFields.People
)

interface Ratable {
	val officialRating: String?
	val communityRating: Float?
	val criticsRating: Float?
}

sealed class BaseItem(original: BaseItemDto) : ObservableParent() {
	val id: String = original.id
	val title: String = original.name
	val titleOriginal: String? = original.originalTitle
	val description: String? = original.overview
	val images: ImageCollection = ImageCollection(original)
	val added: Date = original.dateCreated
	var favorite: Boolean by Delegates.observable(original.userData.isFavorite, ::observer)
	val deletable: Boolean = original.canDelete
	val genres: List<GenreDto> = original.genreItems.toList()
	val tags: List<String> = original.tags
}

sealed class PlayableItem(original: BaseItemDto) : BaseItem(original) {
	var playbackPositionTicks: Long by Delegates.observable(original.userData.playbackPositionTicks, ::observer)
	val mediaInfo = MediaInfo(original.mediaSources, original.mediaStreams)
	val chapters: List<ChapterInfo> = original.chapters.orEmpty().mapIndexed { index, chapterInfoDto ->
		ChapterInfo(chapterInfoDto, this, index)
	}
	var played: Boolean by Delegates.observable(original.userData.played, ::observer)
	val durationTicks: Long? = original.runTimeTicks

	val canResume: Boolean
		get() = playbackPositionTicks > 0L
}

class Episode(original: BaseItemDto) : PlayableItem(original), Ratable {
	val seasonId: String? = original.seasonId
	val seriesId: String? = original.seriesId
	val seriesPrimaryImage: ImageCollection.Image? = original.seriesPrimaryImageTag?.let {
		ImageCollection.Image(original.seriesId, org.jellyfin.apiclient.model.entities.ImageType.Primary, it)
	}
	val seasonPrimaryImage: ImageCollection.Image? = original.seasonId?.let {
		ImageCollection.Image(it, org.jellyfin.apiclient.model.entities.ImageType.Primary, "")
	}

	override val officialRating: String? = original.officialRating
	override val communityRating: Float? = original.communityRating
	override val criticsRating: Float? = original.criticRating

	val cast: List<BriefPersonData> = original.people.map(::BriefPersonData)
	val premiereDate: Date? = original.premiereDate
}

class Movie(original: BaseItemDto, externalTrailerLifter: BaseTrailerLifter) : PlayableItem(original), Ratable {
	var productionYear: Int? = original.productionYear
	val cast: List<BriefPersonData> = original.people.map(::BriefPersonData)

	override val officialRating: String? = original.officialRating
	override val communityRating: Float? = original.communityRating
	override val criticsRating: Float? = original.criticRating

	val externalTrailers: List<ExternalTrailer> = original.remoteTrailers.mapNotNull { externalTrailerLifter.lift(it) }
}

class Series(original: BaseItemDto) : BaseItem(original), Ratable {
	var productionYear: Int? = original.productionYear
	val cast: List<BriefPersonData> = original.people.map(::BriefPersonData)

	override val officialRating: String? = original.officialRating
	override val communityRating: Float? = original.communityRating
	override val criticsRating: Float? = original.criticRating
}

class LocalTrailer(original: BaseItemDto) : PlayableItem(original)

class Video(original: BaseItemDto) : PlayableItem(original)

class Album(original: BaseItemDto) : BaseItem(original), Ratable {
	val artist: List<NameIdPair> = original.artistItems.toList()

	override val officialRating: String? = original.officialRating
	override val communityRating: Float? = original.communityRating
	override val criticsRating: Float? = original.criticRating
}

class Artist(original: BaseItemDto) : BaseItem(original), Ratable {
	override val officialRating: String? = original.officialRating
	override val communityRating: Float? = original.communityRating
	override val criticsRating: Float? = original.criticRating
}

class Audio(original: BaseItemDto) : PlayableItem(original) {
	val index: Int = original.indexNumber
	val artists: List<String> = original.artists.toList()
}
