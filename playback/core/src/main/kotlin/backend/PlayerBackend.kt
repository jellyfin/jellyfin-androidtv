package org.jellyfin.playback.core.backend

import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.model.PositionInfo
import org.jellyfin.playback.core.support.PlaySupportReport
import kotlin.time.Duration

/**
 * Implementation for a media player backend. A backend is unaware of queues and can only play or
 * preload items.
 */
interface PlayerBackend {
	// Testing
	fun supportsStream(stream: MediaStream): PlaySupportReport

	// Data retrieval

	fun setListener(eventListener: PlayerBackendEventListener?)
	fun getPositionInfo(): PositionInfo

	// Mutation

	fun prepareStream(stream: PlayableMediaStream)
	fun playStream(stream: PlayableMediaStream)

	fun play()
	fun pause()
	fun stop()

	fun seekTo(position: Duration)

	fun setSpeed(speed: Float)
}

