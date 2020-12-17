package org.jellyfin.androidtv.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.jellyfin.androidtv.auth.model.*
import org.jellyfin.androidtv.util.apiclient.callApi
import org.jellyfin.androidtv.util.apiclient.getPublicUsers
import org.jellyfin.androidtv.util.apiclient.toPublicUser
import org.jellyfin.androidtv.util.apiclient.toServer
import org.jellyfin.androidtv.util.toUUID
import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.discovery.DiscoveryServerInfo
import org.jellyfin.apiclient.interaction.device.IDevice
import org.jellyfin.apiclient.model.system.PublicSystemInfo
import timber.log.Timber
import java.util.*

interface ServerRepository {
	fun getServers(discovery: Boolean = true, stored: Boolean = true): Flow<Set<Server>>
	fun getServersWithUsers(discovery: Boolean = true, stored: Boolean = true): Flow<Map<Server, Set<User>>>

	fun removeServer(serverId: UUID)
	fun addServer(address: String): Flow<ServerAdditionState>
}

class ServerRepositoryImpl(
	private val jellyfin: Jellyfin,
	private val device: IDevice,
	private val authenticationRepository: AuthenticationRepository,
	private val authenticationStore: AuthenticationStore,
	private val legacyAccountMigration: LegacyAccountMigration
) : ServerRepository {
	@OptIn(ExperimentalCoroutinesApi::class)
	private fun getDiscoveryServers(): Flow<Server> = flow {
		jellyfin.discovery.discover()
			.map(DiscoveryServerInfo::toServer)
			.collect(::emit)
	}.flowOn(Dispatchers.IO)

	private fun getStoredServers(): Flow<Server> = flow {
		authenticationRepository.getServers().forEach { server -> emit(server) }
	}

	private fun getPublicUsersForServer(server: Server): Flow<PublicUser> = flow {
		jellyfin.createApi(server.address, device = device).getPublicUsers()?.forEach { userDto ->
			emit(userDto.toPublicUser())
		}
	}

	private fun getStoredUsersForServer(server: Server): Flow<PrivateUser> = flow {
		authenticationRepository.getUsers(server.id)?.forEach { user -> emit(user) }
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun getServers(discovery: Boolean, stored: Boolean): Flow<Set<Server>> = flow {
		// Migrate old servers and users to new store
		legacyAccountMigration.migrate()

		// Start by emitting an empty collection
		val servers = mutableSetOf<Server>()
		emit(servers)

		// Add discovered servers
		if (discovery) getDiscoveryServers().collect { server ->
			// Only add if not already found in storage
			if (servers.any { it.id == server.id }) {
				servers.add(server)
				emit(servers)
			}
		}

		// Add stored servers
		if (stored) getStoredServers().collect { server ->
			// Remove existing server with id
			// only happens for servers added via discovery
			servers.removeAll { it.id == server.id }
			servers += server
			emit(servers)
		}
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	private suspend fun getUsers(server: Server): Set<User> {
		val users = mutableSetOf<User>()

		getPublicUsersForServer(server).collect { user ->
			// Only add if not already found in storage
			if (users.any { it.id == user.id }) {
				users.add(user)
			}
		}

		getStoredUsersForServer(server).collect { user ->
			// Remove existing server with id
			// only happens for servers added via discovery
			users.removeAll { it.id == user.id }
			users += user
		}

		return users
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	//TODO ALWAYS retrieve users to make way simpler code
	override fun getServersWithUsers(discovery: Boolean, stored: Boolean): Flow<Map<Server, Set<User>>> = flow {
		val serversWithUsers = mutableMapOf<Server, Set<User>>()
		emit(serversWithUsers)

		getServers(discovery, stored).collect { servers ->
			servers.forEach { server ->
				if (server !in serversWithUsers) serversWithUsers[server] = getUsers(server)
			}

			emit(serversWithUsers)
		}
	}

	override fun removeServer(serverId: UUID) {
		authenticationStore.removeServer(serverId)
	}

	override fun addServer(address: String): Flow<ServerAdditionState> = flow {
		Timber.d("Adding server %s", address)

		emit(ConnectingState)

		try {
			val api = jellyfin.createApi(serverAddress = address, device = device)
			val systemInfo: PublicSystemInfo = callApi { callback ->
				api.GetPublicSystemInfoAsync(callback)
			}

			authenticationRepository.saveServer(systemInfo.id.toUUID(), systemInfo.serverName, address)

			emit(ConnectedState(systemInfo))
		} catch (error: Exception) {
			emit(UnableToConnectState(error))
		}
	}
}
