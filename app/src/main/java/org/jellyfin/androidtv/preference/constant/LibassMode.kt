package org.jellyfin.androidtv.preference.constant

import io.github.peerless2012.ass.media.type.AssRenderType

enum class LibassMode {
	MEDIA_CUES,
	MEDIA_EFFECT,
	MEDIA_EFFECT_GL,
	DISABLED;

	val renderType
		get() = when (this) {
			MEDIA_CUES -> AssRenderType.LEGACY
			MEDIA_EFFECT -> AssRenderType.CANVAS
			MEDIA_EFFECT_GL -> AssRenderType.OPEN_GL
			else -> null
		}
}
