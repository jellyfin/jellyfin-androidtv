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
import org.jellyfin.androidtv.util.asLiveDataCollection
import java.util.*

class LoginViewModel(
	private val serverRepository: ServerRepository,
	private val authenticationRepository: AuthenticationRepository,
) : ViewModel() {
	// All available servers and users
	private val _servers = serverRepository.getServersWithUsers(
		discovery = true,
		stored = true
	).asLiveDataCollection()
	val servers: LiveData<List<Pair<Server, List<User>>>> get() = _servers

	fun addServer(address: String): LiveData<ServerAdditionState> = serverRepository.addServer(address).asLiveData()

	fun authenticate(user: User, server: Server): LiveData<LoginState> = authenticationRepository.authenticateUser(user, server).asLiveData()

	fun login(serverId: UUID, username: String, password: String): LiveData<LoginState> = authenticationRepository.login(serverId, username, password).asLiveData()
}
