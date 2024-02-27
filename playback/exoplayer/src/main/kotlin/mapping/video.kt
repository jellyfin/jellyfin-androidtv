package org.jellyfin.playback.exoplayer.mapping

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
fun getFfmpegVideoMimeType(codec: String): String {
	return ffmpegVideoMimeTypes[codec]
		?: MimeTypes.getVideoMediaMimeType(codec)
		?: codec
}

val ffmpegVideoMimeTypes = mapOf<String, String>(
	// TODO: Add map
)
