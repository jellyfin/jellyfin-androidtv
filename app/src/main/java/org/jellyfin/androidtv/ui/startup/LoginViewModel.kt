package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.*
import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.data.model.LoadingState
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.data.repository.ServerRepository
import org.jellyfin.androidtv.data.repository.UserRepository
import org.jellyfin.androidtv.util.apiclient.toUser
import org.jellyfin.androidtv.util.toUUID
import timber.log.Timber
import java.util.*

class LoginViewModel(
	private val serverRepository: ServerRepository,
	private val userRepository: UserRepository,
	private val authenticationRepository: AuthenticationRepository
) : ViewModel() {
	// Currently authenticated user
	private val _currentUser = MutableLiveData<User>()
	val currentUser: LiveData<User> get() = _currentUser

	// Currently authenticated server
	private val _currentServer = MutableLiveData<Server>()
	val currentServer: LiveData<Server> get() = _currentServer

	// Loading state of all data
	private val _loadingState = MutableLiveData<LoadingState>()
	val loadingState: LiveData<LoadingState> get() = _loadingState

	// All available servers + users
	private val _serverList = MediatorLiveData<List<Server>>()
	val serverList: LiveData<List<Server>> get() = _serverList

	private val serverMapUsers = mutableMapOf<UUID, MutableList<User>>()
	val serverMap get() = serverList.map(::getUsers)

	init {
		_serverList.apply {
			value = mutableListOf()

			// Add all the server data
			addSource(serverRepository.discoverServers()) {
				value = value!!.union(it).toMutableList()
			}

			addSource(serverRepository.getServers()) {
				value = value!!.union(it).toMutableList()

				// Set to success after first batch of received servers
				_loadingState.value = LoadingState.SUCCESS
			}
		}
	}

	private fun getUsers(servers: List<Server>) = servers.map { server ->
		val id = server.id.toUUID()

		if (id !in serverMapUsers) {
			val users = mutableListOf<User>()
			users += runBlocking { userRepository.getUsers(server) } // TODO no runblocking
			users += authenticationRepository.getUsersByServer(id) ?: emptyList()

			serverMapUsers[id] = users
		}

		server to (serverMapUsers[id]!! as List<User>)
	}.toMap()

	// Connect to server
	suspend fun connect(address: String) {
		@Suppress("TooGenericExceptionCaught")
		try {
			Timber.d("Connecting to server %s", address)
			val connectResult = serverRepository.connect(address)
			Timber.d("Connected to server %s %s", connectResult.name, connectResult.address)
			_currentServer.postValue(connectResult)
			if (!serverList.value!!.contains(connectResult))
				_serverList.postValue((_serverList.value!! + connectResult).toMutableList())
		} catch (ex: RuntimeException) {
			Timber.w(ex, "Failed to connect to server $address")
			// TODO: Show error messaging
		}
	}

	suspend fun login(server: Server, username: String, password: String) {
		@Suppress("TooGenericExceptionCaught")
		try {
			val authResult = userRepository.login(server, username, password)
			Timber.d("User authenticated %s %s", authResult.user.name, authResult.accessToken)
//			_currentServer.postValue(server)
			_currentUser.postValue(authResult.user.toUser())
			authenticationRepository.login(server, username, password)
		} catch (ex: RuntimeException) {
			Timber.w(ex, "Failed to login as user $username")
			// TODO: Show error messaging
		}
	}
}
