package org.jellyfin.playback.core.mediastream

sealed interface MediaConversionMethod {
	data object None : MediaConversionMethod
	data object Remux : MediaConversionMethod
	data object Transcode : MediaConversionMethod
}
