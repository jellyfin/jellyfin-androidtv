package org.jellyfin.playback.exoplayer.support

import com.google.android.exoplayer2.RendererCapabilities

enum class FormatSupport {
	HANDLED,
	EXCEEDS_CAPABILITIES,
	UNSUPPORTED_DRM,
	UNSUPPORTED_SUBTYPE,
	UNSUPPORTED_TYPE;

	companion object {
		fun fromFlags(flags: Int) = when (RendererCapabilities.getFormatSupport(flags)) {
			RendererCapabilities.FORMAT_HANDLED -> HANDLED
			RendererCapabilities.FORMAT_EXCEEDS_CAPABILITIES -> EXCEEDS_CAPABILITIES
			RendererCapabilities.FORMAT_UNSUPPORTED_DRM -> UNSUPPORTED_DRM
			RendererCapabilities.FORMAT_UNSUPPORTED_SUBTYPE -> UNSUPPORTED_SUBTYPE
			RendererCapabilities.FORMAT_UNSUPPORTED_TYPE -> UNSUPPORTED_TYPE
			else -> null
		}
	}
}
