package org.jellyfin.playback.media3.exoplayer.mapping

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
fun getFfmpegSubtitleMimeType(codec: String, fallback: String = codec): String = codec.lowercase().let { codec ->
	ffmpegSubtitleMimeTypes[codec]
		?: MimeTypes.getTextMediaMimeType(codec)
		?: fallback
}

@OptIn(UnstableApi::class)
val ffmpegSubtitleMimeTypes = mapOf(
	"mp4" to MimeTypes.VIDEO_MP4,
	"ass" to MimeTypes.TEXT_SSA,
	"dvbsub" to MimeTypes.APPLICATION_DVBSUBS,
	"idx" to MimeTypes.APPLICATION_VOBSUB,
	"pgs" to MimeTypes.APPLICATION_PGS,
	"pgssub" to MimeTypes.APPLICATION_PGS,
	"srt" to MimeTypes.APPLICATION_SUBRIP,
	"ssa" to MimeTypes.TEXT_SSA,
	"subrip" to MimeTypes.APPLICATION_SUBRIP,
	"vtt" to MimeTypes.TEXT_VTT,
	"ttml" to MimeTypes.APPLICATION_TTML,
	"webvtt" to MimeTypes.TEXT_VTT,
)
