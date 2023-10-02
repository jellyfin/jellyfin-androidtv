package org.jellyfin.playback.exoplayer.support

import com.google.android.exoplayer2.RendererCapabilities

enum class AdaptiveSupport {
	SEAMLESS,
	NOT_SEAMLESS,
	NOT_SUPPORTED;

	companion object {
		fun fromFlags(flags: Int) = when (RendererCapabilities.getAdaptiveSupport(flags)) {
			RendererCapabilities.ADAPTIVE_SEAMLESS -> SEAMLESS
			RendererCapabilities.ADAPTIVE_NOT_SEAMLESS -> NOT_SEAMLESS
			RendererCapabilities.ADAPTIVE_NOT_SUPPORTED -> NOT_SUPPORTED
			else -> null
		}
	}
}
