package org.jellyfin.playback.exoplayer.support

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.exoplayer.mapping.getFfmpegAudioMimeType
import org.jellyfin.playback.exoplayer.mapping.getFfmpegContainerMimeType

@OptIn(UnstableApi::class)
fun MediaStream.toFormat() = Format.Builder().also { f ->
	f.setId(identifier)
	f.setContainerMimeType(getFfmpegContainerMimeType(container.format))

	val audioTrack = tracks.filterIsInstance<MediaStreamAudioTrack>().firstOrNull()
	if (audioTrack != null) {
		f.setSampleMimeType(getFfmpegAudioMimeType(audioTrack.codec))
		f.setChannelCount(audioTrack.channels)
		f.setAverageBitrate(audioTrack.bitrate)
		f.setSampleRate(audioTrack.sampleRate)
	}
}.build()
