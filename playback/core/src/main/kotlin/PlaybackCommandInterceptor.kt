package org.jellyfin.playback.core

import kotlin.time.Duration

enum class PlaybackSeekCommand { POSITION, PREVIOUS, NEXT }

/** Intercepts user-originated commands from external control surfaces such as MediaSession. */
interface PlaybackCommandInterceptor {
	fun setPlaying(playing: Boolean): Boolean
	fun stop(): Boolean
	fun seek(position: Duration, command: PlaybackSeekCommand): Boolean
	fun setSpeed(speed: Float): Boolean
	fun setShuffle(enabled: Boolean): Boolean
	fun setRepeat(enabled: Boolean): Boolean
}
