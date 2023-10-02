package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamContainer
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType

fun JellyfinStreamResolver.MediaInfo.getMediaStreamContainer() = MediaStreamContainer(
	format = requireNotNull(mediaSource.container)
)

fun JellyfinStreamResolver.MediaInfo.getTracks() =
	mediaSource.mediaStreams
		.orEmpty()
		.mapNotNull(MediaStream::getMediaStreamTrack)

fun MediaStream.getMediaStreamTrack() = when (type) {
	MediaStreamType.AUDIO -> getAudioTrack(this)
	MediaStreamType.VIDEO -> getVideooTrack(this)
	MediaStreamType.SUBTITLE -> getSubtitleTrack(this)

	// Ignore other track types
	MediaStreamType.EMBEDDED_IMAGE,
	MediaStreamType.DATA -> null
}

private fun getAudioTrack(stream: MediaStream) = MediaStreamAudioTrack(
	codec = requireNotNull(stream.codec),
	bitrate = stream.bitRate ?: 0,
	channels = stream.channels ?: 1,
	sampleRate = stream.sampleRate ?: 0,
)

// TODO Implement Video track type
private fun getVideooTrack(stream: MediaStream) = null

// TODO Implement Subtitle track type
private fun getSubtitleTrack(stream: MediaStream) = null
