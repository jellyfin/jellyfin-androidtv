package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.auth.ServerRepository
import org.jellyfin.androidtv.auth.model.AuthenticationSortBy
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.LoginState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.User
import org.jellyfin.androidtv.preference.AuthenticationPreferences
import java.util.UUID

class LoginViewModel(
	private val serverRepository: ServerRepository,
	private val authenticationRepository: AuthenticationRepository,
	private val authenticationPreferences: AuthenticationPreferences,
) : ViewModel() {
	val discoveredServers: Flow<Server>
		get() = serverRepository.getDiscoveryServers()

	private val _storedServers = MutableStateFlow<List<Server>>(emptyList())
	val storedServers: StateFlow<List<Server>>
		get() = _storedServers

	private val _users = MutableStateFlow<List<User>>(emptyList())
	val users: StateFlow<List<User>>
		get() = _users

	fun getServer(id: UUID) = serverRepository.getStoredServers()
		.find { it.id == id }

	fun loadUsers(server: Server) {
		viewModelScope.launch {
			serverRepository.getServerUsers(server).map { users ->
				if (authenticationPreferences[AuthenticationPreferences.sortBy] == AuthenticationSortBy.ALPHABETICAL)
					users.sortedBy { user -> user.name }
				else users
			}.collect { users ->
				_users.value = users
			}
		}
	}

	fun addServer(address: String) = serverRepository.addServer(address).onEach {
		// Reload stored servers when new server is added
		if (it is ConnectedState) reloadServers()
	}

	fun removeServer(serverId: UUID) {
		val removed = serverRepository.removeServer(serverId)

		// Reload stored servers when server is removed
		if (removed) _storedServers.value = serverRepository.getStoredServers()
	}

	fun authenticate(user: User, server: Server): Flow<LoginState> =
		authenticationRepository.authenticateUser(user, server)

	fun login(server: Server, username: String, password: String): Flow<LoginState> =
		authenticationRepository.login(server, username, password)

	fun getUserImage(server: Server, user: User): String? =
		authenticationRepository.getUserImageUrl(server, user)

	fun reloadServers() {
		val servers = serverRepository.getStoredServers().let { servers ->
			if (authenticationPreferences[AuthenticationPreferences.sortBy] == AuthenticationSortBy.ALPHABETICAL)
				servers.sortedBy { it.name + it.address }
			else servers
		}

		_storedServers.value = servers
	}

	fun getLastServer(): Server? =
		serverRepository.getStoredServers().maxByOrNull { it.dateLastAccessed }

	suspend fun updateServer(server: Server): Boolean =
		serverRepository.refreshServerInfo(server)
}
