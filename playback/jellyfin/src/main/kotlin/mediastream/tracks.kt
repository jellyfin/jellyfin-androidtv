package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.playback.core.mediastream.ExternalSubtitle
import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamContainer
import org.jellyfin.playback.core.mediastream.MediaStreamVideoTrack
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod

fun MediaInfo.getMediaStreamContainer() = MediaStreamContainer(
	format = requireNotNull(mediaSource.container)
)

fun MediaInfo.getTracks() =
	mediaSource.mediaStreams
		.orEmpty()
		.mapNotNull(MediaStream::getMediaStreamTrack)

fun MediaInfo.getExternalSubtitles(api: ApiClient): List<ExternalSubtitle> =
	mediaSource.mediaStreams
		.orEmpty()
		.filter { it.type == MediaStreamType.SUBTITLE && it.deliveryMethod == SubtitleDeliveryMethod.EXTERNAL }
		.mapNotNull { stream ->
			val deliveryUrl = stream.deliveryUrl ?: return@mapNotNull null
			ExternalSubtitle(
				url = api.createUrl(deliveryUrl, ignorePathParameters = true),
				mimeType = getSubtitleMimeType(stream.codec),
				language = stream.language,
				title = stream.displayTitle,
				index = stream.index,
			)
		}

private fun getSubtitleMimeType(codec: String?): String = when (codec?.lowercase()) {
	"srt", "subrip" -> "application/x-subrip"
	"ass", "ssa" -> "text/x-ssa"
	"vtt", "webvtt" -> "text/vtt"
	"ttml" -> "application/ttml+xml"
	else -> "application/x-subrip" // default to SRT
}

fun MediaStream.getMediaStreamTrack() = when (type) {
	MediaStreamType.AUDIO -> getAudioTrack(this)
	MediaStreamType.VIDEO -> getVideoTrack(this)
	MediaStreamType.SUBTITLE -> getSubtitleTrack(this)

	// Ignore other track types
	MediaStreamType.EMBEDDED_IMAGE,
	MediaStreamType.DATA,
	MediaStreamType.LYRIC -> null
}

private fun getAudioTrack(stream: MediaStream) = MediaStreamAudioTrack(
	codec = requireNotNull(stream.codec),
	bitrate = stream.bitRate ?: 0,
	channels = stream.channels ?: 1,
	sampleRate = stream.sampleRate ?: 0,
)

private fun getVideoTrack(stream: MediaStream) = MediaStreamVideoTrack(
	codec = requireNotNull(stream.codec),
)

// TODO Implement Subtitle track type
private fun getSubtitleTrack(stream: MediaStream) = null
