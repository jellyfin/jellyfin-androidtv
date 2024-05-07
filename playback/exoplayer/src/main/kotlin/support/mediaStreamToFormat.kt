package org.jellyfin.playback.exoplayer.support

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamVideoTrack
import org.jellyfin.playback.exoplayer.mapping.getFfmpegAudioMimeType
import org.jellyfin.playback.exoplayer.mapping.getFfmpegContainerMimeType
import org.jellyfin.playback.exoplayer.mapping.getFfmpegVideoMimeType

@OptIn(UnstableApi::class)
fun toFormat(stream: MediaStream, track: MediaStreamAudioTrack) = Format.Builder().also { f ->
	f.setId(stream.identifier)
	f.setContainerMimeType(getFfmpegContainerMimeType(stream.container.format))

	f.setCodecs(track.codec)
	f.setSampleMimeType(getFfmpegAudioMimeType(track.codec))
	f.setChannelCount(track.channels)
	f.setAverageBitrate(track.bitrate)
	f.setSampleRate(track.sampleRate)
}.build()

@OptIn(UnstableApi::class)
fun toFormat(stream: MediaStream, track: MediaStreamVideoTrack) = Format.Builder().also { f ->
	f.setId(stream.identifier)
	f.setContainerMimeType(getFfmpegContainerMimeType(stream.container.format))

	f.setCodecs(track.codec)
	f.setSampleMimeType(getFfmpegVideoMimeType(track.codec))
}.build()

fun MediaStream.toFormats() = tracks.map { track ->
	when (track) {
		is MediaStreamAudioTrack -> toFormat(stream = this, track)
		is MediaStreamVideoTrack -> toFormat(stream = this, track)
	}
}
