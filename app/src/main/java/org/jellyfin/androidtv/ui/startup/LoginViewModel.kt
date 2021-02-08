package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.auth.ServerRepository
import org.jellyfin.androidtv.auth.model.LoginState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.ServerAdditionState
import org.jellyfin.androidtv.auth.model.User

class LoginViewModel(
	private val serverRepository: ServerRepository,
	private val authenticationRepository: AuthenticationRepository,
) : ViewModel() {
	// All available servers and users
	private var _servers = serverRepository.getServersWithUsers(
		discovery = true,
		stored = true
	).asLiveData()
	val servers: LiveData<Map<Server, Set<User>>> get() = _servers

	fun addServer(address: String): LiveData<ServerAdditionState> = serverRepository.addServer(address).asLiveData()

	fun authenticate(user: User, server: Server): LiveData<LoginState> = authenticationRepository.authenticateUser(user, server).asLiveData()

	fun login(server: Server, username: String, password: String): LiveData<LoginState> = authenticationRepository.login(server, username, password).asLiveData()

	fun getUserImage(server: Server, user: User): String? = authenticationRepository.getUserImageUrl(server, user)

	fun refreshServers() {
		_servers = serverRepository.getServersWithUsers(
				discovery = true,
				stored = true
		).asLiveData()
	}
}
