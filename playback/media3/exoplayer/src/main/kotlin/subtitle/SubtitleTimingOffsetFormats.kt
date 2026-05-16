package org.jellyfin.playback.media3.exoplayer.subtitle

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

@UnstableApi
fun isSubtitleTimingOffsetSupported(format: Format): Boolean = when (format.sampleMimeType) {
	MimeTypes.APPLICATION_SUBRIP,
	MimeTypes.TEXT_VTT,
	MimeTypes.TEXT_SSA,
	MimeTypes.APPLICATION_TTML -> true
	else -> false
}
