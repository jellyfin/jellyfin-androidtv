package org.jellyfin.playback.media3.exoplayer

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource

data class ExoPlayerOptions(
	val preferFfmpeg: Boolean = false,
	val enableDebugLogging: Boolean = false,
	val baseDataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory(),
)
