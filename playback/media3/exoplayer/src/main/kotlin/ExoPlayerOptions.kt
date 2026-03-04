package org.jellyfin.playback.media3.exoplayer

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource

data class ExoPlayerOptions(
	val preferFfmpeg: Boolean = false,
	val enableDebugLogging: Boolean = false,
	val enableLibass: Boolean = false,
	val libassGlyphSize: Int = 0,
	val libassCacheSizeMB: Int = 0,
	val baseDataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory(),
)
