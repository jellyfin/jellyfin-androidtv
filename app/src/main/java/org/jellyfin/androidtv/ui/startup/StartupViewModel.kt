package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jellyfin.androidtv.auth.model.AuthenticationSortBy
import org.jellyfin.androidtv.auth.model.AutomaticAuthenticateMethod
import org.jellyfin.androidtv.auth.model.LoginState
import org.jellyfin.androidtv.auth.model.PrivateUser
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.User
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import java.util.UUID

/**
 * Data class to represent a user with their associated server information
 */
data class UserWithServerInfo(
	val user: User,
	val server: Server
)

class StartupViewModel(
	private val serverRepository: ServerRepository,
	private val serverUserRepository: ServerUserRepository,
	private val authenticationRepository: AuthenticationRepository,
	private val authenticationPreferences: AuthenticationPreferences,
) : ViewModel() {
	val storedServers = serverRepository.storedServers
	val discoveredServers = serverRepository.discoveredServers

	private val _users = MutableStateFlow<List<User>>(emptyList())
	val users = _users.asStateFlow()

	// New state for all users across all servers
	private val _allUsers = MutableStateFlow<List<UserWithServerInfo>>(emptyList())
	val allUsers = _allUsers.asStateFlow()

	// Add mode to control user display - reactive to preference changes
	private val _showAllServers = MutableStateFlow(authenticationPreferences[AuthenticationPreferences.showAllServersUsers])
	val showAllServers = _showAllServers.asStateFlow()

	// Method to refresh the preference state from storage
	fun refreshShowAllServersPreference() {
		val currentValue = authenticationPreferences[AuthenticationPreferences.showAllServersUsers]
		if (_showAllServers.value != currentValue) {
			_showAllServers.value = currentValue
		}
	}

	private val userComparator = compareByDescending<User> { user ->
		if (
			authenticationPreferences[AuthenticationPreferences.sortBy] == AuthenticationSortBy.LAST_USE &&
			user is PrivateUser
		) user.lastUsed
		else null
	}.thenBy { user -> user.name }

	private val discoveryMutex = Mutex()

	fun getServer(id: UUID) = serverRepository.storedServers.value
		.find { it.id == id }

	fun loadUsers(server: Server) {
		viewModelScope.launch {
			val storedUsers = serverUserRepository.getStoredServerUsers(server)
			_users.value = storedUsers.sortedWith(userComparator)

			val storedUserIds = storedUsers.map { it.id }
			val publicUsers = serverUserRepository.getPublicServerUsers(server)
				.filterNot { it.id in storedUserIds }
			_users.value = (storedUsers + publicUsers).sortedWith(userComparator)
		}
	}

	/**
	 * Load users from all stored servers for cross-server user selection
	 */
	fun loadAllUsers() {
		viewModelScope.launch {
			val allUsersWithServerInfo = mutableListOf<UserWithServerInfo>()
			
			for (server in serverRepository.storedServers.value) {
				val storedUsers = serverUserRepository.getStoredServerUsers(server)
				val publicUsers = serverUserRepository.getPublicServerUsers(server)
				val allUsersForServer = (storedUsers + publicUsers).distinctBy { it.id }
				
				allUsersForServer.forEach { user ->
					allUsersWithServerInfo.add(UserWithServerInfo(user, server))
				}
			}
			
			_allUsers.value = allUsersWithServerInfo.sortedWith(
				compareByDescending<UserWithServerInfo> { userWithServer ->
					if (
						authenticationPreferences[AuthenticationPreferences.sortBy] == AuthenticationSortBy.LAST_USE &&
						userWithServer.user is PrivateUser
					) userWithServer.user.lastUsed
					else null
				}.thenBy { it.user.name }.thenBy { it.server.name }
			)
		}
	}

	fun addServer(address: String) = serverRepository.addServer(address)

	fun deleteServer(serverId: UUID) {
		viewModelScope.launch { serverRepository.deleteServer(serverId) }
	}

	fun authenticate(server: Server, user: User): Flow<LoginState> =
		authenticationRepository.authenticate(server, AutomaticAuthenticateMethod(user))

	fun authenticate(userWithServer: UserWithServerInfo): Flow<LoginState> =
		authenticationRepository.authenticate(userWithServer.server, AutomaticAuthenticateMethod(userWithServer.user))

	fun getUserImage(server: Server, user: User): String? =
		authenticationRepository.getUserImageUrl(server, user)

	fun loadDiscoveryServers() {
		// Only run one discovery process at a time
		if (discoveryMutex.isLocked) return

		viewModelScope.launch(Dispatchers.IO) {
			discoveryMutex.withLock {
				serverRepository.loadDiscoveryServers()
			}
		}
	}

	fun reloadStoredServers() {
		viewModelScope.launch { serverRepository.loadStoredServers() }
	}

	suspend fun getLastServer(): Server? {
		serverRepository.loadStoredServers()
		return serverRepository.storedServers.value.maxByOrNull { it.dateLastAccessed }
	}

	suspend fun updateServer(server: Server): Boolean = serverRepository.updateServer(server)

	fun toggleServerMode() {
		_showAllServers.value = !_showAllServers.value
	}

	fun setShowAllServers(showAll: Boolean) {
		_showAllServers.value = showAll
		authenticationPreferences[AuthenticationPreferences.showAllServersUsers] = showAll
	}
}

