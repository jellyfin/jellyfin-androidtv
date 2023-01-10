package org.jellyfin.playback.core.plugin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.PlayerState

abstract class PlayerService {
	private lateinit var _manager: PlaybackManager
	private lateinit var _state: PlayerState
	private lateinit var _job: Job
	private lateinit var _coroutineScope: CoroutineScope

	internal fun initialize(manager: PlaybackManager, state: PlayerState, job: Job) {
		_manager = manager
		_state = state
		_job = job
		_coroutineScope = CoroutineScope(_job)

		_coroutineScope.launch {
			onInitialize()
		}
	}

	val manager: PlaybackManager get() = _manager
	val state: PlayerState get() = _state
	val coroutineScope: CoroutineScope get() = _coroutineScope

	open suspend fun onInitialize() = Unit
}
