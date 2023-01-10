package org.jellyfin.playback.core.backend

/**
 * A base class that implements the event listening part of [PlayerBackend].
 */
abstract class BasePlayerBackend : PlayerBackend {
	private var _listener: PlayerBackendEventListener? = null
	protected val listener: PlayerBackendEventListener? get() = _listener

	override fun setListener(eventListener: PlayerBackendEventListener?) {
		_listener = eventListener
	}
}
