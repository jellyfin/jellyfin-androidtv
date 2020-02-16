package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.androidtv.model.itemtypes.*
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType

fun BaseItemDto.liftToNewFormat() : BaseItem {
	return when(baseItemType) {
		// Movies
		BaseItemType.Movie -> Movie(this)

		// TV
		BaseItemType.Episode -> Episode(this)

		// Video, like making-ofs and interviews
		BaseItemType.Video -> Video(this)

		BaseItemType.Trailer -> Trailer(this)

		else -> TODO()
	}
}
