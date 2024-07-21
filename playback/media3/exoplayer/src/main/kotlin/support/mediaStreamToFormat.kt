package org.jellyfin.playback.media3.exoplayer.support

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamVideoTrack
import org.jellyfin.playback.media3.exoplayer.mapping.getFfmpegAudioMimeType
import org.jellyfin.playback.media3.exoplayer.mapping.getFfmpegContainerMimeType
import org.jellyfin.playback.media3.exoplayer.mapping.getFfmpegVideoMimeType

@OptIn(UnstableApi::class)
private val PCM_CODECS = mapOf(
	"pcm_s8" to C.ENCODING_PCM_8BIT,
	"pcm_s16le" to C.ENCODING_PCM_16BIT,
	"pcm_s16be" to C.ENCODING_PCM_16BIT_BIG_ENDIAN,
	"pcm_s24le" to C.ENCODING_PCM_24BIT,
	"pcm_s24be" to C.ENCODING_PCM_24BIT_BIG_ENDIAN,
	"pcm_s32le" to C.ENCODING_PCM_32BIT,
	"pcm_s32be" to C.ENCODING_PCM_32BIT_BIG_ENDIAN,
	"pcm_f32le" to C.ENCODING_PCM_FLOAT,
)

@OptIn(UnstableApi::class)
fun toFormat(stream: MediaStream, track: MediaStreamAudioTrack) = Format.Builder().also { f ->
	f.setId(stream.identifier)
	f.setContainerMimeType(getFfmpegContainerMimeType(stream.container.format))

	val pcmEncoding = PCM_CODECS[track.codec]
	if (pcmEncoding != null) {
		f.setSampleMimeType(MimeTypes.AUDIO_RAW)
		f.setPcmEncoding(pcmEncoding)
	} else {
		f.setCodecs(track.codec)
		f.setSampleMimeType(getFfmpegAudioMimeType(track.codec))
	}

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
