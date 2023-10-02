package org.jellyfin.playback.core.mediastream

import org.jellyfin.playback.core.queue.item.QueueEntry

data class MediaStream(
	val identifier: String,
	val queueEntry: QueueEntry,
	val conversionMethod: MediaConversionMethod,
	val url: String,
	val container: MediaStreamContainer,
	val tracks: Collection<MediaStreamTrack>,
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

// TODO: Add Video/Subtitle tracks
