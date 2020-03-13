package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.androidtv.model.itemtypes.*
import org.jellyfin.androidtv.model.trailers.lifter.FirstMatchMultiExternalTrailerLifter
import org.jellyfin.androidtv.model.trailers.lifter.GenericExternalTrailerLifter
import org.jellyfin.androidtv.model.trailers.lifter.YouTubeExternalTrailerLifter
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType

//TODO: When DI and Repositories are implemented, move the trailer lifting
val multiTrailerLifter = FirstMatchMultiExternalTrailerLifter().apply {
	addFirst(YouTubeExternalTrailerLifter())
	addLast(GenericExternalTrailerLifter())
}

fun BaseItemDto.liftToNewFormat() : BaseItem {
	return when(baseItemType) {
		// Movies
		BaseItemType.Movie -> Movie(this, multiTrailerLifter)

		// TV
		BaseItemType.Episode -> Episode(this)

		// Video, like making-ofs and interviews
		BaseItemType.Video -> Video(this)

		BaseItemType.Trailer -> LocalTrailer(this)

		else -> TODO()
	}
}
