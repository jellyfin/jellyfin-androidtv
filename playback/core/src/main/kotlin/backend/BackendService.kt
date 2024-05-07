package org.jellyfin.playback.core.backend

import android.view.SurfaceView
import androidx.core.view.doOnDetach
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.model.PlayState

/**
 * Service keeping track of the current playback backend and its related surface view.
 */
class BackendService {
	private var _backend: PlayerBackend? = null
	val backend get() = _backend

	private var listeners = mutableListOf<PlayerBackendEventListener>()
	private var _surfaceView: SurfaceView? = null

	fun switchBackend(backend: PlayerBackend) {
		_backend?.stop()
		_backend?.setListener(null)
		_backend?.setSurface(null)

		_backend = backend.apply {
			_surfaceView?.let(::setSurface)
			setListener(BackendEventListener())
		}
	}

	fun attachSurfaceView(surfaceView: SurfaceView) {
		// Remove existing surface view
		if (_surfaceView != null) {
			_backend?.setSurface(null)
		}

		// Apply new surface view
		_surfaceView = surfaceView.apply {
			_backend?.setSurface(surfaceView)

			// Automatically detach
			doOnDetach {
				if (surfaceView == _surfaceView) {
					_surfaceView = null
					_backend?.setSurface(null)
				}
			}
		}
	}

	fun addListener(listener: PlayerBackendEventListener) {
		listeners.add(listener)
	}

	fun removeListener(listener: PlayerBackendEventListener) {
		listeners.remove(listener)
	}

	inner class BackendEventListener : PlayerBackendEventListener {
		private fun <T> callListeners(
			body: PlayerBackendEventListener.() -> T
		): List<T> = listeners.map { listener -> listener.body() }

		override fun onPlayStateChange(state: PlayState) {
			callListeners { onPlayStateChange(state) }
		}

		override fun onVideoSizeChange(width: Int, height: Int) {
			callListeners { onVideoSizeChange(width, height) }
		}

		override fun onMediaStreamEnd(mediaStream: PlayableMediaStream) {
			callListeners { onMediaStreamEnd(mediaStream) }
		}
	}
}
