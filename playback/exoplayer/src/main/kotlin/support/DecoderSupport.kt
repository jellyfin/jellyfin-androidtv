package org.jellyfin.playback.exoplayer.support

import com.google.android.exoplayer2.RendererCapabilities

enum class DecoderSupport {
	PRIMARY,
	FALLBACK_MIMETYPE,
	FALLBACK;

	companion object {
		fun fromFlags(flags: Int) = when (RendererCapabilities.getDecoderSupport(flags)) {
			RendererCapabilities.DECODER_SUPPORT_PRIMARY -> PRIMARY
			RendererCapabilities.DECODER_SUPPORT_FALLBACK_MIMETYPE -> FALLBACK_MIMETYPE
			RendererCapabilities.DECODER_SUPPORT_FALLBACK -> FALLBACK
			else -> null
		}
	}
}
