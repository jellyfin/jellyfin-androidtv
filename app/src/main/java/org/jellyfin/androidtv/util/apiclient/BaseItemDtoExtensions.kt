package org.jellyfin.androidtv.util.apiclient

import android.graphics.Bitmap
import org.jellyfin.androidtv.model.itemtypes.Episode
import org.jellyfin.apiclient.model.dto.BaseItemDto

fun BaseItemDto.asEpisode(primaryImage: Bitmap?) : Episode {
	return Episode(id = id,
		canDelete = canDelete,
		canDownload = canDownload,
		communityRating = communityRating,
		name = name,
		overview = overview,
		primaryImage = primaryImage)
}
