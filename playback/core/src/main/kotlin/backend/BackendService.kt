package org.jellyfin.playback.core.backend

import androidx.core.view.doOnDetach
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.ui.PlayerSubtitleView
import org.jellyfin.playback.core.ui.PlayerSurfaceView
import kotlin.time.Duration

/**
 * Service keeping track of the current playback backend and its related surface view.
 */
class BackendService {
	private var _backend: PlayerBackend? = null
	val backend get() = _backend

	private var listeners = mutableListOf<PlayerBackendEventListener>()
	private var _surfaceView: PlayerSurfaceView? = null
	private val _subtitleViews = mutableListOf<PlayerSubtitleView>()

	fun switchBackend(backend: PlayerBackend) {
		_backend?.stop()
		_backend?.setListener(null)
		_backend?.setSurfaceView(null)
		_backend?.setSubtitleViews(emptyList())

		_backend = backend.apply {
			_surfaceView?.let(::setSurfaceView)
			setSubtitleViews(_subtitleViews.toList())
			setListener(BackendEventListener())
		}
	}

	fun attachSurfaceView(surfaceView: PlayerSurfaceView) {
		// Remove existing surface view
		if (_surfaceView != null) {
			_backend?.setSurfaceView(null)
		}

		// Apply new surface view
		_surfaceView = surfaceView.apply {
			_backend?.setSurfaceView(surfaceView)

			// Automatically detach
			doOnDetach {
				if (surfaceView == _surfaceView) {
					_surfaceView = null
					_backend?.setSurfaceView(null)
				}
			}
		}
	}

	fun attachSubtitleView(subtitleView: PlayerSubtitleView) {
		if (subtitleView in _subtitleViews) return

		_subtitleViews.add(subtitleView)
		syncSubtitleViews()

		subtitleView.doOnDetach {
			if (_subtitleViews.remove(subtitleView)) {
				syncSubtitleViews()
			}
		}
	}

	private fun syncSubtitleViews() {
		_backend?.setSubtitleViews(_subtitleViews.toList())
	}

	fun addListener(listener: PlayerBackendEventListener) {
		listeners.add(listener)
	}

	fun removeListener(listener: PlayerBackendEventListener) {
		listeners.remove(listener)
	}

	inner class BackendEventListener : PlayerBackendEventListener() {
		private fun <T> callListeners(
			body: PlayerBackendEventListener.() -> T
		): List<T> = listeners.map { listener -> listener.body() }

		override fun onPlayStateChange(state: PlayState, stream: PlayableMediaStream?, position: Duration?) {
			callListeners { onPlayStateChange(state, stream, position) }
		}

		override fun onVideoSizeChange(width: Int, height: Int) {
			callListeners { onVideoSizeChange(width, height) }
		}

		override fun onMediaStreamEnd(mediaStream: PlayableMediaStream, position: Duration) {
			callListeners { onMediaStreamEnd(mediaStream, position) }
		}

		override fun onMediaStreamReplaced(mediaStream: PlayableMediaStream, position: Duration) {
			callListeners { onMediaStreamReplaced(mediaStream, position) }
		}
	}
}
