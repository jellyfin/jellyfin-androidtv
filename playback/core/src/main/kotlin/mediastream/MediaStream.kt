package org.jellyfin.playback.core.mediastream

import org.jellyfin.playback.core.queue.QueueEntry

interface MediaStream {
	val identifier: String
	val conversionMethod: MediaConversionMethod
	val container: MediaStreamContainer
	val tracks: Collection<MediaStreamTrack>
}

data class BasicMediaStream(
	override val identifier: String,
	override val conversionMethod: MediaConversionMethod,
	override val container: MediaStreamContainer,
	override val tracks: Collection<MediaStreamTrack>,
	val externalSubtitles: List<ExternalSubtitle> = emptyList(),
) : MediaStream {
	fun toPlayableMediaStream(
		queueEntry: QueueEntry,
		url: String,
	) = PlayableMediaStream(
		identifier = identifier,
		conversionMethod = conversionMethod,
		container = container,
		tracks = tracks,
		queueEntry = queueEntry,
		url = url,
		externalSubtitles = externalSubtitles,
	)
}

data class PlayableMediaStream(
	override val identifier: String,
	override val conversionMethod: MediaConversionMethod,
	override val container: MediaStreamContainer,
	override val tracks: Collection<MediaStreamTrack>,
	val queueEntry: QueueEntry,
	val url: String,
	val externalSubtitles: List<ExternalSubtitle> = emptyList(),
) : MediaStream

data class ExternalSubtitle(
	val url: String,
	val mimeType: String,
	val language: String?,
	val title: String?,
	val index: Int,
)

data class MediaStreamContainer(
	val format: String,
)

sealed interface MediaStreamTrack {
	val codec: String
}

data class MediaStreamAudioTrack(
	override val codec: String,
	val bitrate: Int,
	val channels: Int,
	val sampleRate: Int,
) : MediaStreamTrack

data class MediaStreamVideoTrack(
	override val codec: String,
) : MediaStreamTrack

// TODO: Add subtitle track
