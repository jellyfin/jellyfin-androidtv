package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.util.toUUIDOrNull
import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.interaction.device.IDevice
import org.jellyfin.apiclient.model.system.PublicSystemInfo
import java.util.*

interface ServerRepository {
	fun getServers(discovery: Boolean = true, stored: Boolean = true, legacy: Boolean = true): Flow<Server>
	fun getServersWithUsers(discovery: Boolean = true, stored: Boolean = true, legacy: Boolean = true): Flow<Pair<Server, List<User>>>

	fun removeServer(serverId: UUID)
	fun addServer(url: String): Flow<ServerAdditionState>
}

sealed class ServerAdditionState
object ConnectingState : ServerAdditionState()
object UnableToConnectState : ServerAdditionState()
data class ConnectedState(val publicInfo: PublicSystemInfo) : ServerAdditionState()

class ServerRepositoryImpl(
	private val jellyfin: Jellyfin,
	private val device: IDevice,
	private val authenticationRepository: AuthenticationRepository
) : ServerRepository {
	private fun getDiscoveryServers(): Flow<Server> {
		// TODO
		return emptyFlow()
	}

	private fun getStoredServers(): Flow<Server> = flow {
		authenticationRepository.getServers().forEach { server -> emit(server) }
	}

	private fun getLegacyServers(): Flow<Server> {
		// TODO
		return emptyFlow()
	}

	private fun getPublicUsersForServer(server: Server): Flow<User> {
		// TODO
		return emptyFlow()
	}

	private fun getStoredUsersForServer(server: Server): Flow<User> = flow {
		val id = server.id.toUUIDOrNull() ?: return@flow

		authenticationRepository.getUsersByServer(id)?.forEach { user -> emit(user) }
	}

	private fun getLegacyUsersForServer(server: Server): Flow<User> {
		// TODO
		return emptyFlow()
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun getServers(discovery: Boolean, stored: Boolean, legacy: Boolean): Flow<Server> = flow {
		if (discovery) emitAll(getDiscoveryServers())
		if (stored) emitAll(getStoredServers())
		if (legacy) emitAll(getLegacyServers())
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun getServersWithUsers(discovery: Boolean, stored: Boolean, legacy: Boolean): Flow<Pair<Server, List<User>>> = getServers(discovery, stored, legacy).map { server ->
		val users = flow {
			emitAll(getPublicUsersForServer(server))
			if (stored) emitAll(getStoredUsersForServer(server))
			if (legacy) emitAll(getLegacyUsersForServer(server))
		}.toList()

		Pair(server, users)
	}

	override fun removeServer(serverId: UUID) {
		TODO("Not yet implemented")
	}

	override fun addServer(url: String): Flow<ServerAdditionState> {
		TODO("Not yet implemented")
	}
}
