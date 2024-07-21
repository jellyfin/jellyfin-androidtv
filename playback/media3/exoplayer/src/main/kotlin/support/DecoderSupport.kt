package org.jellyfin.playback.media3.exoplayer.support

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.RendererCapabilities

enum class DecoderSupport {
	PRIMARY,
	FALLBACK_MIMETYPE,
	FALLBACK;

	companion object {
		@OptIn(UnstableApi::class)
		fun fromFlags(flags: Int) = when (RendererCapabilities.getDecoderSupport(flags)) {
			RendererCapabilities.DECODER_SUPPORT_PRIMARY -> PRIMARY
			RendererCapabilities.DECODER_SUPPORT_FALLBACK_MIMETYPE -> FALLBACK_MIMETYPE
			RendererCapabilities.DECODER_SUPPORT_FALLBACK -> FALLBACK
			else -> null
		}
	}
}
