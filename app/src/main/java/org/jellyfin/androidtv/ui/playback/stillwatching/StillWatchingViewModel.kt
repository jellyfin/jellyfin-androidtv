package org.jellyfin.androidtv.ui.playback.stillwatching

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StillWatchingViewModel : ViewModel() {
	private val _state = MutableStateFlow(StillWatchingState.INITIALIZED)
	val state: StateFlow<StillWatchingState> = _state

	fun stillWatching() {
		_state.value = StillWatchingState.STILL_WATCHING
	}

	fun close() {
		_state.value = StillWatchingState.CLOSE
	}
}

