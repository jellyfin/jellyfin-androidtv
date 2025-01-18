package org.jellyfin.playback.media3.exoplayer.mapping

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
fun getFfmpegVideoMimeType(codec: String, fallback: String = codec) = codec.lowercase().let { codec ->
	ffmpegVideoMimeTypes[codec]
		?: MimeTypes.getVideoMediaMimeType(codec)
		?: fallback
}

@OptIn(UnstableApi::class)
val ffmpegVideoMimeTypes = mapOf(
	"mp4" to MimeTypes.VIDEO_MP4,
	"mkv" to MimeTypes.VIDEO_MATROSKA,
	"webm" to MimeTypes.VIDEO_WEBM,
	"h263" to MimeTypes.VIDEO_H263,
	"h264" to MimeTypes.VIDEO_H264,
	"hevc" to MimeTypes.VIDEO_H265,
	"vp8" to MimeTypes.VIDEO_VP8,
	"vp9" to MimeTypes.VIDEO_VP9,
	"av1" to MimeTypes.VIDEO_AV1,
	"mpeg" to MimeTypes.VIDEO_MPEG,
	"mp2" to MimeTypes.VIDEO_MPEG2,
	"vc1" to MimeTypes.VIDEO_VC1,
	"flv" to MimeTypes.VIDEO_FLV,
	"ogv" to MimeTypes.VIDEO_OGG,
	"avi" to MimeTypes.VIDEO_AVI,
	"mjpeg" to MimeTypes.VIDEO_MJPEG,
	"rawvideo" to MimeTypes.VIDEO_RAW,
)
