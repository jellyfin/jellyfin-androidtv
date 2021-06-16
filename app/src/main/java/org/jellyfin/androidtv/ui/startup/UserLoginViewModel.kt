package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.model.AuthenticatingState
import org.jellyfin.androidtv.auth.model.AutomaticAuthenticateMethod
import org.jellyfin.androidtv.auth.model.ConnectedQuickConnectState
import org.jellyfin.androidtv.auth.model.CredentialAuthenticateMethod
import org.jellyfin.androidtv.auth.model.LoginState
import org.jellyfin.androidtv.auth.model.PendingQuickConnectState
import org.jellyfin.androidtv.auth.model.QuickConnectAuthenticateMethod
import org.jellyfin.androidtv.auth.model.QuickConnectState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.UnavailableQuickConnectState
import org.jellyfin.androidtv.auth.model.UnknownQuickConnectState
import org.jellyfin.androidtv.auth.model.User
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.quickConnectApi
import timber.log.Timber
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class UserLoginViewModel(
	jellyfin: Jellyfin,
	private val serverRepository: ServerRepository,
	private val authenticationRepository: AuthenticationRepository,
) : ViewModel() {
	private val _loginState = MutableStateFlow<LoginState?>(null)
	val loginState = _loginState.asStateFlow()

	var forcedUsername: String? = null

	private val _server = MutableStateFlow<Server?>(null)
	val server = _server.asStateFlow()

	private val quickConnectApi = jellyfin.createApi()
	private var quickConnectSecret: String? = null
	private val _quickConnectState = MutableStateFlow<QuickConnectState>(UnknownQuickConnectState)
	val quickConnectState = _quickConnectState.asStateFlow()
	fun authenticate(server: Server, user: User): Flow<LoginState> =
		authenticationRepository.authenticate(server, AutomaticAuthenticateMethod(user))

	fun login(username: String, password: String) {
		val server = server.value ?: return
		_loginState.value = AuthenticatingState
		viewModelScope.launch {
			authenticationRepository.authenticate(server, CredentialAuthenticateMethod(username, password)).collect {
				_loginState.value = it
			}
		}
	}

	fun clearLoginState() {
		_loginState.value = null
	}

	/**
	 * Start a new Quick Connect flow. Does nothing when already active.
	 */
	suspend fun initiateQuickconnect() {
		// Already initialized
		if (quickConnectState.value != UnknownQuickConnectState) return

		val server = server.value ?: return
		_quickConnectState.emit(UnknownQuickConnectState)
		quickConnectSecret = null
		quickConnectApi.baseUrl = server.address

		try {
			val response by quickConnectApi.quickConnectApi.initiate()

			quickConnectSecret = response.secret
			_quickConnectState.emit(PendingQuickConnectState(response.code))
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to initiate QuickConnect")
			_quickConnectState.emit(UnavailableQuickConnectState)
		}

		// Update every 5 seconds until QuickConnect is inactive or the view model is cancelled
		viewModelScope.launch {
			while (isActive) {
				delay(5.seconds)
				if (!updateQuickConnectState()) break
			}
		}
	}

	/**
	 * Update the Quick Connect state.
	 *
	 * @return true when Quick Connect is active, false when inactive.
	 */
	private suspend fun updateQuickConnectState(): Boolean {
		val server = server.value ?: return false
		val secret = quickConnectSecret ?: return false

		try {
			val state by quickConnectApi.quickConnectApi.connect(secret = secret)

			if (state.authenticated) {
				_quickConnectState.emit(ConnectedQuickConnectState)

				authenticationRepository.authenticate(server, QuickConnectAuthenticateMethod(state.secret)).collect {
					_loginState.emit(it)
				}

				return false
			} else {
				_quickConnectState.emit(PendingQuickConnectState(state.code))
				return true
			}
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to initiate QuickConnect")
			_quickConnectState.emit(UnavailableQuickConnectState)
			return false
		}
	}

	fun setServer(id: UUID?) {
		_server.value = serverRepository.storedServers.value
			.firstOrNull { it.id == id }
	}
}
