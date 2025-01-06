package org.jellyfin.androidtv.ui.playback

import androidx.core.net.toUri
import org.jellyfin.playback.media3.exoplayer.mapping.getFfmpegSubtitleMimeType
import org.jellyfin.sdk.model.api.MediaStream

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
