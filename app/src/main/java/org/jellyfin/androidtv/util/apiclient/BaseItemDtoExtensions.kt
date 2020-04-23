package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.androidtv.model.itemtypes.*
import org.jellyfin.androidtv.model.trailers.lifter.FirstMatchTrailerLifter
import org.jellyfin.androidtv.model.trailers.lifter.GenericTrailerLifter
import org.jellyfin.androidtv.model.trailers.lifter.YouTubeTrailerLifter
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType

//TODO: When DI and Repositories are implemented, move the trailer lifting
val multiTrailerLifter = FirstMatchTrailerLifter().apply {
	addFirst(YouTubeTrailerLifter())
	addLast(GenericTrailerLifter())
}

fun BaseItemDto.liftToNewFormat(): BaseItem {
	return when (baseItemType) {
		// Movies
		BaseItemType.Movie -> Movie(this, multiTrailerLifter)

		// TV
		BaseItemType.Episode -> Episode(this)

		// Video, like making-ofs and interviews
		BaseItemType.Video -> Video(this)

		BaseItemType.Trailer -> LocalTrailer(this)

		// Music
		BaseItemType.MusicAlbum -> Album(this)
		BaseItemType.MusicArtist -> Artist(this)
		BaseItemType.Audio -> Audio(this)

		else -> TODO()
	}
}
