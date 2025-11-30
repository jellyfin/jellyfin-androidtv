package org.jellyfin.playback.media3.exoplayer

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource

/**
 * Subtitle display mode determining when subtitles are shown.
 * Maps to org.jellyfin.sdk.model.api.SubtitlePlaybackMode from the Jellyfin SDK.
 */
enum class SubtitleMode {
	/** Let the server/player decide based on audio language vs subtitle language match */
	DEFAULT,
	/** Show subtitles only when they are marked as forced */
	ONLY_FORCED,
	/** Always show subtitles if available */
	ALWAYS,
	/** Never show subtitles */
	NONE,
	/** Smart mode - show subtitles based on audio/subtitle language matching */
	SMART,
}

data class ExoPlayerOptions(
	val preferFfmpeg: Boolean = false,
	val enableDebugLogging: Boolean = false,
	val baseDataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory(),
	val preferredAudioLanguage: String? = null,
	val preferredSubtitleLanguage: String? = null,
	val subtitleMode: SubtitleMode = SubtitleMode.DEFAULT,
)
