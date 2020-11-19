package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.*
import org.jellyfin.androidtv.auth.AccountRepository
import org.jellyfin.androidtv.data.model.LoadingState
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.ServerList
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.data.repository.ServerRepository
import org.jellyfin.androidtv.data.repository.UserRepository
import org.jellyfin.androidtv.util.apiclient.toUser
import timber.log.Timber

class LoginViewModel(
	private val serverRepository: ServerRepository,
	private val userRepository: UserRepository,
	accountRepository: AccountRepository
) : ViewModel() {
	private val _currentServer = MutableLiveData<Server>()
	val currentServer: LiveData<Server>
		get() = _currentServer

	private val _currentUser = MutableLiveData<User>()
	val currentUser: LiveData<User>
		get() = _currentUser

	private val _serverList = MediatorLiveData<ServerList>()
	val serverList: LiveData<ServerList>
		get() = _serverList

	private val _loadingCurrentServer = MutableLiveData<LoadingState>()
	private val currentServerUsers = _currentServer.switchMap { server ->
		liveData {
			_loadingCurrentServer.value = LoadingState.LOADING
			emit(hashMapOf(server to userRepository.getUsers(server)))
			_loadingCurrentServer.value = LoadingState.SUCCESS
		}
	}

	private val _loadingDiscoveredServers = MutableLiveData<LoadingState>()
	private val discoveredServers = liveData {
		_loadingDiscoveredServers.value = LoadingState.LOADING
		emit(serverRepository.discoverServers()
			.associateWith { userRepository.getUsers(it) })
		_loadingDiscoveredServers.value = LoadingState.SUCCESS
	}

	private val _loadingSavedServers = MutableLiveData<LoadingState>()
	private val savedServers = liveData {
		_loadingSavedServers.value = LoadingState.LOADING
		emit(serverRepository.getServers()
			.associateWith { userRepository.getUsers(it) })
		_loadingSavedServers.value = LoadingState.SUCCESS
	}

	init {
		_serverList.apply {
			// Add all loading states
			addSource(_loadingCurrentServer) {
				value = (value ?: ServerList()).apply { currentServerUsersState = it }
			}
			addSource(_loadingDiscoveredServers) {
				value = (value ?: ServerList()).apply { discoveredServersUsersState = it }
			}
			addSource(_loadingSavedServers) {
				value = (value ?: ServerList()).apply { savedServersUsersState = it }
			}

			addSource(MutableLiveData(accountRepository.getAccounts().map { Server(it.key, it.key, it.key) to it.value.map { User(it.username, it.username, it.accessToken ?: "", it.server) } }.toMap())) {
//				value = (value ?: ServerList()).apply { currentServerUsers = it }
				value = (value ?: ServerList()).apply { savedServersUsers = it }
			}

			// Add all the server data
			addSource (currentServerUsers) {
				value = (value ?: ServerList()).apply { currentServerUsers = it }
			}
				addSource (discoveredServers) {
				value = (value ?: ServerList()).apply { discoveredServersUsers = it }
			}
//				addSource (savedServers) {
////				value = (value ?: ServerList()).apply { savedServersUsers = it }
//			}
		}
	}

	suspend fun connect(address: String) {
		@Suppress("TooGenericExceptionCaught")
		try {
			Timber.d("Connecting to server %s", address)
			val connectResult = serverRepository.connect(address)
			Timber.d("Connected to server %s %s", connectResult.name, connectResult.address)
			_currentServer.postValue(connectResult)
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
			_currentServer.postValue(server)
			_currentUser.postValue(authResult.user.toUser())
		} catch (ex: RuntimeException) {
			Timber.w(ex, "Failed to login as user $username")
			// TODO: Show error messaging
		}
	}
}
