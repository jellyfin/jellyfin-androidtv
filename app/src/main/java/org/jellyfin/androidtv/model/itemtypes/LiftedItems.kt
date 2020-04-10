package org.jellyfin.androidtv.model.itemtypes

import org.jellyfin.androidtv.model.trailers.external.ExternalTrailer
import org.jellyfin.androidtv.model.trailers.lifter.BaseTrailerLifter
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.GenreDto
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

sealed class BaseItem(original: BaseItemDto) : ObservableParent() {
	val id: String = original.id
	val title: String = original.name
	val titleOriginal: String? = original.originalTitle
	val description: String? = original.overview
	val images: ImageCollection = ImageCollection(original)
	val added: Date = original.dateCreated
	var favorite: Boolean by Delegates.observable(original.userData.isFavorite, ::observer)
	val deletable: Boolean = original.canDelete
}

sealed class PlayableItem(original: BaseItemDto) : BaseItem(original) {
	var playbackPositionTicks: Long by Delegates.observable(original.userData.playbackPositionTicks, ::observer)
	val mediaInfo = MediaInfo(original.mediaSources, original.mediaStreams)
	val chapters: List<ChapterInfo> = original.chapters.orEmpty().mapIndexed { index, chapterInfoDto ->
		ChapterInfo(chapterInfoDto, this, index)
	}
	var played: Boolean by Delegates.observable(original.userData.played, ::observer)
	val genres: List<GenreDto> = original.genreItems.toList()
	val tags: List<String> = original.tags
	val durationTicks: Long? = original.runTimeTicks

	val canResume: Boolean
		get() = playbackPositionTicks > 0L
}

class Episode(original: BaseItemDto) : PlayableItem(original) {
	val seasonId: String? = original.seasonId
	val seriesId: String? = original.seriesId
	val seriesPrimaryImage: ImageCollection.Image? = original.seriesPrimaryImageTag?.let {
		 ImageCollection.Image(original.seriesId, org.jellyfin.apiclient.model.entities.ImageType.Primary, it)
	}
	val seasonPrimaryImage: ImageCollection.Image? = original.seasonId?.let {
		ImageCollection.Image(it, org.jellyfin.apiclient.model.entities.ImageType.Primary, "")
	}
	val cast: List<BriefPersonData> = original.people.map(::BriefPersonData)
}

class Movie(original: BaseItemDto, externalTrailerLifter: BaseTrailerLifter) : PlayableItem(original) {
	var productionYear: Int? = original.productionYear
	val cast: List<BriefPersonData> = original.people.map(::BriefPersonData)
	val officialRating: String? = original.officialRating
	val communityRating: Float? = original.communityRating
	val criticsRating: Float? = original.criticRating
	val localTrailerCount: Int = original.localTrailerCount
	val externalTrailers: List<ExternalTrailer> = original.remoteTrailers.mapNotNull { externalTrailerLifter.lift(it) }
}

class LocalTrailer(original: BaseItemDto) : PlayableItem(original)

class Video(original: BaseItemDto) : PlayableItem(original)
