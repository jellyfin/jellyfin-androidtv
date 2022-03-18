package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.auth.model.AuthenticationSortBy
import org.jellyfin.androidtv.auth.ServerRepository
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

	private val _storedServers = MutableLiveData<List<Server>>()
	val storedServers: LiveData<List<Server>>
		get() = _storedServers

	private val _users = MediatorLiveData<List<User>>()
	val users: LiveData<List<User>>
		get() = _users

	fun getServer(id: UUID) = serverRepository.getStoredServers()
		.find { it.id == id }

	fun loadUsers(server: Server) {
		val source = serverRepository.getServerUsers(server).map { users ->
			if (authenticationPreferences[AuthenticationPreferences.sortBy] == AuthenticationSortBy.ALPHABETICAL)
				users.sortedBy { user -> user.name }
			else users
		}

		_users.addSource(source) {
			_users.value = source.value
		}
	}

	fun addServer(address: String) = liveData {
		serverRepository.addServer(address).onEach {
			// Reload stored servers when new server is added
			if (it is ConnectedState) reloadServers()

			emit(it)
		}.collect()
	}

	fun removeServer(serverId: UUID) {
		val removed = serverRepository.removeServer(serverId)

		// Reload stored servers when server is removed
		if (removed) _storedServers.postValue(serverRepository.getStoredServers())
	}

	fun authenticate(user: User, server: Server): LiveData<LoginState> =
		authenticationRepository.authenticateUser(user, server).asLiveData()

	fun login(server: Server, username: String, password: String): LiveData<LoginState> =
		authenticationRepository.login(server, username, password).asLiveData()

	fun getUserImage(server: Server, user: User): String? =
		authenticationRepository.getUserImageUrl(server, user)

	fun reloadServers() {
		val servers = serverRepository.getStoredServers().let { servers ->
			if (authenticationPreferences[AuthenticationPreferences.sortBy] == AuthenticationSortBy.ALPHABETICAL)
				servers.sortedBy { it.name + it.address }
			else servers
		}

		_storedServers.postValue(servers)
	}

	fun getLastServer(): Server? =
		serverRepository.getStoredServers().maxByOrNull { it.dateLastAccessed }

	suspend fun updateServer(server: Server): Boolean =
		serverRepository.refreshServerInfo(server)
}
