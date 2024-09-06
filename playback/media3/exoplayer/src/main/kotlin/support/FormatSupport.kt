package org.jellyfin.playback.media3.exoplayer.support

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.RendererCapabilities

enum class FormatSupport {
	HANDLED,
	EXCEEDS_CAPABILITIES,
	UNSUPPORTED_DRM,
	UNSUPPORTED_SUBTYPE,
	UNSUPPORTED_TYPE;

	companion object {
		@OptIn(UnstableApi::class)
		fun fromFlags(flags: Int) = when (RendererCapabilities.getFormatSupport(flags)) {
			C.FORMAT_HANDLED -> HANDLED
			C.FORMAT_EXCEEDS_CAPABILITIES -> EXCEEDS_CAPABILITIES
			C.FORMAT_UNSUPPORTED_DRM -> UNSUPPORTED_DRM
			C.FORMAT_UNSUPPORTED_SUBTYPE -> UNSUPPORTED_SUBTYPE
			C.FORMAT_UNSUPPORTED_TYPE -> UNSUPPORTED_TYPE
			else -> null
		}
	}
}
