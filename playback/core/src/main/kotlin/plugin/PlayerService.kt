package org.jellyfin.playback.core.plugin

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.PlayerState

enum class PlayerServiceType {
	/**
	 * Foreground service - only active when app is in the foreground (RESUMED state).
	 * Suitable for UI-related services like WebSocket handlers for remote control.
	 */
	FOREGROUND,
	
	/**
	 * Background service - always active regardless of app lifecycle state.
	 * Suitable for core playback services that need to continue when app is backgrounded.
	 */
	BACKGROUND
}

/**
 * Marker interface for PlayerService implementations that should only be active
 * when the app is in the foreground (RESUMED state).
 */
interface ForegroundPlayerService

abstract class PlayerService {
	private lateinit var _manager: PlaybackManager
	private lateinit var _state: PlayerState
	private lateinit var _job: Job
	private lateinit var _coroutineScope: CoroutineScope
	private var _lifecycle: Lifecycle? = null
	private var _serviceType: PlayerServiceType = PlayerServiceType.BACKGROUND

	internal fun initialize(
		manager: PlaybackManager, 
		state: PlayerState, 
		job: Job,
		lifecycle: Lifecycle? = null,
		serviceType: PlayerServiceType = PlayerServiceType.BACKGROUND
	) {
		_manager = manager
		_state = state
		_job = job
		_coroutineScope = CoroutineScope(_job)
		_lifecycle = lifecycle
		_serviceType = serviceType

		when (_serviceType) {
			PlayerServiceType.BACKGROUND -> {
				_coroutineScope.launch {
					onInitialize()
				}
			}
			PlayerServiceType.FOREGROUND -> {
				if (_lifecycle != null) {
					_lifecycle.lifecycleScope.launch {
						_lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
							onInitialize()
						}
					}
				} else {
					// Fallback to background behavior if no lifecycle provided
					_coroutineScope.launch {
						onInitialize()
					}
				}
			}
		}
	}

	val manager: PlaybackManager get() = _manager
	val state: PlayerState get() = _state
	val coroutineScope: CoroutineScope get() = _coroutineScope
	val lifecycle: Lifecycle? get() = _lifecycle
	val serviceType: PlayerServiceType get() = _serviceType

	open suspend fun onInitialize() = Unit
}
