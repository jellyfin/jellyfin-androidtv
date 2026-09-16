package org.jellyfin.androidtv.ui.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.video.MediaCodecVideoDecoderException
import java.util.concurrent.CopyOnWriteArraySet

// Tracks video decoders that fail at runtime (crash mid-stream, after already initializing
// successfully) so they can be excluded from future selection.
@UnstableApi
class FailedVideoDecoderTracker {
	private val excludedNames = CopyOnWriteArraySet<String>()

	fun clear() = excludedNames.clear()

	fun filter(decoderInfos: List<MediaCodecInfo>): List<MediaCodecInfo> =
		if (excludedNames.isEmpty()) decoderInfos else decoderInfos.filterNot { it.name in excludedNames }

	fun tryExcludeFailedDecoder(error: Throwable): Boolean {
		var cause: Throwable? = error
		while (cause != null) {
			if (cause is MediaCodecVideoDecoderException) {
				val codecInfo = cause.codecInfo ?: return false
				return excludedNames.add(codecInfo.name)
			}
			cause = cause.cause
		}
		return false
	}
}
