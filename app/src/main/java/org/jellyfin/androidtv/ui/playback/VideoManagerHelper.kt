package org.jellyfin.androidtv.ui.playback

import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.os.Build
import androidx.core.net.toUri
import org.jellyfin.playback.media3.exoplayer.mapping.getFfmpegSubtitleMimeType
import org.jellyfin.sdk.model.api.MediaStream
import timber.log.Timber

/**
 * Return the media type for the codec found in this media stream. First tries to infer the media type from the streams delivery URL and
 * falls back to the original stream codec.
 */
fun getSubtitleMediaStreamCodec(stream: MediaStream): String {
	val codec = requireNotNull(stream.codec)
	val codecMediaType = getFfmpegSubtitleMimeType(codec, "").ifBlank { null }

	val urlSubtitleExtension = stream.deliveryUrl?.toUri()?.lastPathSegment?.split('.')?.last()
	val urlExtensionMediaType = urlSubtitleExtension?.let { getFfmpegSubtitleMimeType(it, "") }?.ifBlank { null }

	return urlExtensionMediaType ?: codecMediaType ?: urlSubtitleExtension ?: codec
}

private var audioEffect: AudioEffect? = null

fun applyAudioNightmode(audioSessionId: Int) {
	Timber.i("Enabling audio night mode for session $audioSessionId")

	audioEffect?.release()

	audioEffect = when {
		// Use dynamics processinc on Android 9 (API 28) and newer
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> DynamicsProcessing(0, audioSessionId, null).apply {
			setLimiterAllChannelsTo(
				DynamicsProcessing.Limiter(
					true,
					true,
					1,
					30f,
					300f,
					10f,
					-24f,
					3f
				)
			)

			setPreEqAllChannelsTo(DynamicsProcessing.Eq(true, true, 5).apply {
				getBand(0).gain = 0f
				getBand(1).gain = 0.02f
				getBand(2).gain = 0.03f
				getBand(3).gain = 0.02f
				getBand(4).gain = 0f
			})

			enabled = true
		}

		// Use more simple equalizer implementation on older versions
		else -> Equalizer(0, audioSessionId).apply {
			setBandLevel(0, 0)
			setBandLevel(1, 2)
			setBandLevel(2, 3)
			setBandLevel(3, 2)
			setBandLevel(4, 0)
			enabled = true
		}
	}
}
