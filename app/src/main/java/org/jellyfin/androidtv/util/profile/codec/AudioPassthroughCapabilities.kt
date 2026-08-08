package org.jellyfin.androidtv.util.profile.codec

import android.content.Context
import android.media.AudioFormat
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioCapabilities

class AudioPassthroughCapabilities (
	private val context: Context
){
	@OptIn(UnstableApi::class)
	fun isPassthroughAudioAvailable(mimetype: String): Boolean {
		// Def audio attributes
		val audioAttributes = AudioAttributes.Builder()
			.setUsage(C.USAGE_MEDIA)
			.setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
			.build()
		// Get audio capabilities
		val audioCapabilities = AudioCapabilities.getCapabilities(
			context,
			audioAttributes,
			null,
			listOf(
				AudioFormat.CHANNEL_OUT_STEREO,
				AudioFormat.CHANNEL_OUT_5POINT1
			)
		)
		// Set audio format for a passthrough 2.0 audio codec check
		val format = Format.Builder()
			.setSampleMimeType(mimetype)
			.setChannelCount(Integer.bitCount(AudioFormat.CHANNEL_OUT_STEREO))
			.setSampleRate(Format.NO_VALUE)
			.build()
		// Test Passthrough Direct Playback
		return audioCapabilities.isPassthroughPlaybackSupported(format, audioAttributes)
	}
}
