package org.jellyfin.androidtv.ui.syncplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayClient
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayStatus
import org.jellyfin.sdk.model.api.GroupInfoDto
import java.util.UUID

enum class SyncPlayUiError { GROUPS, MEMBERSHIP, ACTION }

data class SyncPlayUiState(
	val visible: Boolean = false,
	val groups: List<GroupInfoDto> = emptyList(),
	val loading: Boolean = false,
	val error: SyncPlayUiError? = null,
)

class SyncPlayViewModel(
	private val client: SyncPlayClient,
	sessionRepository: SessionRepository,
) : ViewModel() {
	private val mutableUiState = MutableStateFlow(SyncPlayUiState())
	val uiState: StateFlow<SyncPlayUiState> = mutableUiState.asStateFlow()
	val syncPlayState: StateFlow<SyncPlayStatus> = client.state
	private var operation: Job? = null
	private var activeSession = sessionRepository.currentSession.value

	init {
		viewModelScope.launch {
			sessionRepository.currentSession.collectLatest { session ->
				if (session != activeSession) {
					activeSession = session
					operation?.cancel()
					client.close()
					mutableUiState.value = SyncPlayUiState()
				}
			}
		}
	}

	fun show() {
		mutableUiState.value = mutableUiState.value.copy(visible = true)
		refresh()
	}

	fun dismiss() {
		mutableUiState.value = mutableUiState.value.copy(visible = false, error = null)
	}

	fun refresh() = launchOperation(SyncPlayUiError.GROUPS) {
		mutableUiState.value = mutableUiState.value.copy(groups = client.groups())
		true
	}

	fun create(name: String) {
		val normalized = name.trim()
		if (normalized.isEmpty()) return
		launchOperation(SyncPlayUiError.MEMBERSHIP) { client.create(normalized) }
	}

	fun join(groupId: UUID) {
		launchOperation(SyncPlayUiError.MEMBERSHIP) { client.join(groupId) }
	}

	fun leave() {
		launchOperation(SyncPlayUiError.ACTION) {
			client.leave()
			true
		}
	}

	fun halt() {
		launchOperation(SyncPlayUiError.ACTION) {
			client.halt()
			true
		}
	}

	fun resume() {
		launchOperation(SyncPlayUiError.ACTION) {
			client.resume()
			true
		}
	}

	private fun launchOperation(failure: SyncPlayUiError, block: suspend () -> Boolean) {
		if (operation?.isActive == true) return
		mutableUiState.value = mutableUiState.value.copy(loading = true, error = null)
		operation = viewModelScope.launch {
			try {
				if (!block()) mutableUiState.value = mutableUiState.value.copy(error = failure)
			} catch (error: CancellationException) {
				throw error
			} catch (_: Exception) {
				mutableUiState.value = mutableUiState.value.copy(error = failure)
			} finally {
				mutableUiState.value = mutableUiState.value.copy(loading = false)
			}
		}
	}

	override fun onCleared() {
		client.close()
		super.onCleared()
	}
}
