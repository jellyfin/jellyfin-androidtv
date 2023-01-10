package org.jellyfin.playback.core

import org.jellyfin.playback.core.backend.PlayerBackend

/**
 * Repository keeping track of the current playback backend.
 */
class PlaybackRepository {
	private var _backend: PlayerBackend? = null
	val backend get() = _backend

	fun switchBackend(backend: PlayerBackend) {
		_backend = backend
	}
}
