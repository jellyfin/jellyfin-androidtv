package org.jellyfin.playback.media3.exoplayer

import androidx.media3.exoplayer.DefaultLoadControl

enum class ExoPlayerBufferSize(
	val minBufferMs: Int,
	val maxBufferMs: Int,
	val bufferForPlaybackMs: Int,
	val bufferForPlaybackAfterRebufferMs: Int,
) {
	AUTO(
		minBufferMs = DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
		maxBufferMs = DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
		bufferForPlaybackMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
		bufferForPlaybackAfterRebufferMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
	),
	@Suppress("MagicNumber")
	LARGE(
		minBufferMs = 50_000,
		maxBufferMs = 120_000,
		bufferForPlaybackMs = 2_500,
		bufferForPlaybackAfterRebufferMs = 5_000,
	),
	@Suppress("MagicNumber")
	EXTRA_LARGE(
		minBufferMs = 80_000,
		maxBufferMs = 240_000,
		bufferForPlaybackMs = 5_000,
		bufferForPlaybackAfterRebufferMs = 10_000,
	),
}
