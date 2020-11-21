package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.data.model.LoadingState
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.data.repository.ServerRepository
import org.jellyfin.androidtv.data.repository.UserRepository
import org.jellyfin.androidtv.util.apiclient.toUser
import timber.log.Timber

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

	// Discovered servers
	private val discoveredServers = MutableLiveData<Collection<Server>>()

	// Stored servers + users
	private val storedServers = authenticationRepository.getUsers()

	// All available servers + users
	private val _serverList = MutableLiveData<Map<Server, List<User>>>()
	val serverList: LiveData<Map<Server, List<User>>> get() = _serverList

	init {
		_serverList.value = storedServers
		_loadingState.value = LoadingState.SUCCESS
	}

	// Connect to server
	suspend fun connect(address: String) {
		@Suppress("TooGenericExceptionCaught")
		try {
			Timber.d("Connecting to server %s", address)
			val connectResult = serverRepository.connect(address)
			Timber.d("Connected to server %s %s", connectResult.name, connectResult.address)
			_currentServer.postValue(connectResult)
			if (serverList.value?.keys?.contains(connectResult) != true)
				_serverList.postValue(_serverList.value!! + (connectResult to emptyList()))
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
			authenticationRepository.login(server,username,password)
		} catch (ex: RuntimeException) {
			Timber.w(ex, "Failed to login as user $username")
			// TODO: Show error messaging
		}
	}
}
