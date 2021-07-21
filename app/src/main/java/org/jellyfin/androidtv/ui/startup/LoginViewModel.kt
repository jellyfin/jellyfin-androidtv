package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.auth.ServerRepository
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.LoginState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.User
import java.util.*

class LoginViewModel(
	private val serverRepository: ServerRepository,
	private val authenticationRepository: AuthenticationRepository,
) : ViewModel() {
	val discoveredServers: Flow<Server>
		get() = serverRepository.getDiscoveryServers()

	private val _storedServers = MutableLiveData<List<Server>>()
	val storedServers: LiveData<List<Server>>
		get() = _storedServers

	fun getServer(id: UUID) = serverRepository.getStoredServers()
		.find { it.id == id }

	fun getUsers(server: Server) = serverRepository.getServerUsers(server)

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

	fun authenticate(user: User, server: Server): LiveData<LoginState> = authenticationRepository.authenticateUser(user, server).asLiveData()

	fun login(server: Server, username: String, password: String): LiveData<LoginState> = authenticationRepository.login(server, username, password).asLiveData()

	fun getUserImage(server: Server, user: User): String? = authenticationRepository.getUserImageUrl(server, user)

	fun reloadServers() {
		_storedServers.postValue(serverRepository.getStoredServers())
	}

	fun getLastServer(): Server? = serverRepository.getStoredServers()
		.sortedByDescending { it.dateLastAccessed }
		.firstOrNull()

	suspend fun updateServer(server: Server): Boolean = serverRepository.refreshServerInfo(server)
}
