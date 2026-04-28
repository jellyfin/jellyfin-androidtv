package org.jellyfin.playback.media3.exoplayer

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import kotlin.time.Duration

data class ExoPlayerOptions(
	val preferFfmpeg: Boolean = false,
	val enableDebugLogging: Boolean = false,
	val enableLibass: Boolean = false,
	val baseDataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory(),
	val minBufferDuration: Duration? = null,
	val maxBufferDuration: Duration? = null,
	val bufferForPlaybackDuration: Duration? = null,
	val bufferForPlaybackAfterRebufferDuration: Duration? = null,
)
