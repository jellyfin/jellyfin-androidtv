package org.jellyfin.playback.exoplayer

import kotlin.time.Duration

data class ExoPlayerOptions(
	val httpConnectTimeout: Duration? = null,
	val httpReadTimeout: Duration? = null,
)
