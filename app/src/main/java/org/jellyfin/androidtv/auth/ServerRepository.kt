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
		emitAll(jellyfin.discovery.discover().map(DiscoveryServerInfo::toServer))
	}.flowOn(Dispatchers.IO)

	private fun getStoredServers(): Flow<Server> = flow {
		authenticationRepository.getServers().forEach { server -> emit(server) }
	}.flowOn(Dispatchers.IO)

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
		// Migrate old servers and users to new store before reading them from the new store
		legacyAccountMigration.migrate()

		// Start by emitting an empty collection
		val servers = mutableSetOf<Server>()
		val flows = mutableListOf<Flow<Server>>()
		emit(servers)

		// Add discovered servers
		if (discovery) flows += getDiscoveryServers().onEach { server ->
			// Only add if not already found in storage
			if (servers.none { it.id == server.id }) {
				servers.add(server)
				emit(servers)
			}
		}

		// Add stored servers
		if (stored) flows += getStoredServers().onEach { server ->
			// Remove existing server with id
			// only happens for servers added via discovery
			servers.removeAll { it.id == server.id }
			servers += server
			emit(servers)
		}

		// Wait for all flows to complete
		flows.forEach { it.collect() }
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	private suspend fun getUsers(server: Server): Set<User> {
		val users = mutableSetOf<User>()
		val flows = mutableListOf<Flow<User>>()

		flows += getPublicUsersForServer(server).onEach { user ->
			// Only add if not already found in storage
			if (users.none { it.id == user.id }) {
				users.add(user)
			}
		}

		flows += getStoredUsersForServer(server).onEach { user ->
			// Remove existing server with id
			// only happens for servers added via discovery
			users.removeAll { it.id == user.id }
			users += user
		}

		// Wait for all flows to complete
		flows.forEach { it.collect() }

		return users
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun getServersWithUsers(discovery: Boolean, stored: Boolean): Flow<Map<Server, Set<User>>> = flow {
		val userFlows = mutableMapOf<UUID, Set<User>>()

		getServers(discovery, stored).collect { servers ->
			emit(servers.map { server ->
				if (server.id !in userFlows)
					userFlows[server.id] = getUsers(server)

				server to userFlows[server.id]!!
			}.toMap())
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
