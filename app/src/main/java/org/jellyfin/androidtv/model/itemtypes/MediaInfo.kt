package org.jellyfin.androidtv.model.itemtypes

import org.jellyfin.apiclient.model.dto.MediaSourceInfo
import org.jellyfin.apiclient.model.entities.MediaStream

data class MediaInfo(
	val sources: List<MediaSourceInfo>,
	val streams: List<MediaStream>
)
