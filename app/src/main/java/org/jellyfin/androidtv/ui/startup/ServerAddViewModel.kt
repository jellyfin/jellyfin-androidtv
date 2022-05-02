package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.model.ServerAdditionState
import org.jellyfin.androidtv.auth.repository.ServerRepository

class ServerAddViewModel(
	private val serverRepository: ServerRepository,
) : ViewModel() {
	private val _state = MutableStateFlow<ServerAdditionState?>(null)
	val state = _state.asStateFlow()

	fun addServer(address: String) {
		viewModelScope.launch {
			serverRepository.addServer(address).collect { state ->
				_state.value = state
			}
		}
	}
}
