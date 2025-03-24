package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.sdk.model.api.MediaSourceInfo

data class MediaInfo(
	val playSessionId: String,
	val mediaSource: MediaSourceInfo,
)
