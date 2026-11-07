package org.jellyfin.androidtv.util.profile.codec

import android.content.Context
import android.media.AudioFormat
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioCapabilities

@OptIn(UnstableApi::class)
fun isPassthroughAudioAvailable(context: Context, mimetype: String): Boolean {
	val attributes = AudioAttributes.Builder()
		.setUsage(C.USAGE_MEDIA)
		.setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
		.build()

	val capabilities = AudioCapabilities.getCapabilities(
		context,
		attributes,
		null,
		listOf(AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.CHANNEL_OUT_5POINT1)
	)

	val format = Format.Builder()
		.setSampleMimeType(mimetype)
		.setChannelCount(Integer.bitCount(AudioFormat.CHANNEL_OUT_STEREO))
		.setSampleRate(Format.NO_VALUE)
		.build()

	if (capabilities.isPassthroughPlaybackSupported(format, attributes)) {
		return true
	}

	if (mimetype == androidx.media3.common.MimeTypes.AUDIO_DTS) {
		val dtsHdFormat = Format.Builder()
			.setSampleMimeType(androidx.media3.common.MimeTypes.AUDIO_DTS_HD)
			.setChannelCount(Integer.bitCount(AudioFormat.CHANNEL_OUT_STEREO))
			.setSampleRate(Format.NO_VALUE)
			.build()
		if (capabilities.isPassthroughPlaybackSupported(dtsHdFormat, attributes)) {
			return true
		}
	}

	return false
}
