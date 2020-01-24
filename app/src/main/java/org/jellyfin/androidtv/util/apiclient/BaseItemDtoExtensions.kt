package org.jellyfin.androidtv.util.apiclient

import android.graphics.Bitmap
import org.jellyfin.androidtv.model.itemtypes.Episode
import org.jellyfin.apiclient.model.dto.BaseItemDto

fun BaseItemDto.asEpisode() : Episode {
	return Episode(id = id,
		communityRating = communityRating.toDouble(),
		name = name,
		description = overview,
		canResume = canResume,
		playbackPositionTicks = userData.playbackPositionTicks)
}
