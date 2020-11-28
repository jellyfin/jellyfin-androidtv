package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.data.source.CredentialsFileSource
import org.jellyfin.androidtv.util.apiclient.callApi
import org.jellyfin.androidtv.util.apiclient.getPublicUsers
import org.jellyfin.androidtv.util.apiclient.toServer
import org.jellyfin.androidtv.util.apiclient.toUser
import org.jellyfin.androidtv.util.toUUID
import org.jellyfin.androidtv.util.toUUIDOrNull
import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.discovery.DiscoveryServerInfo
import org.jellyfin.apiclient.interaction.device.IDevice
import org.jellyfin.apiclient.model.system.PublicSystemInfo
import timber.log.Timber
import java.util.*

interface ServerRepository {
	fun getServers(discovery: Boolean = true, stored: Boolean = true, legacy: Boolean = true): Flow<Server>
	fun getServersWithUsers(discovery: Boolean = true, stored: Boolean = true, legacy: Boolean = true): Flow<Pair<Server, List<User>>>

	fun removeServer(serverId: UUID)
	fun addServer(address: String): Flow<ServerAdditionState>
}

sealed class ServerAdditionState
object ConnectingState : ServerAdditionState()
data class UnableToConnectState(val error: Exception) : ServerAdditionState()
data class ConnectedState(val publicInfo: PublicSystemInfo) : ServerAdditionState()

sealed class LoginState
object AuthenticatingState : LoginState()
object RequireSignInState : LoginState()
object ServerUnavailableState : LoginState()
object AuthenticatedState : LoginState()

class ServerRepositoryImpl(
	private val jellyfin: Jellyfin,
	private val device: IDevice,
	private val authenticationRepository: AuthenticationRepository,
	private val credentialsFileSource: CredentialsFileSource,
) : ServerRepository {
	@OptIn(ExperimentalCoroutinesApi::class)
	private fun getDiscoveryServers(): Flow<Server> = flow {
		withContext(Dispatchers.IO) {
			emitAll(jellyfin.discovery.discover().map(DiscoveryServerInfo::toServer))
		}
	}

	private fun getStoredServers(): Flow<Server> = flow {
		authenticationRepository.getServers().forEach { server -> emit(server) }
	}

	private fun getLegacyServers(): Flow<Server> = flow {
		val server = credentialsFileSource.read()?.server ?: return@flow
		emit(server)
	}

	private fun getPublicUsersForServer(server: Server): Flow<User> = flow {
		jellyfin.createApi(server.address, device = device).getPublicUsers()?.forEach { userDto ->
			emit(userDto.toUser())
		}
	}

	private fun getStoredUsersForServer(server: Server): Flow<User> = flow {
		val id = server.id.toUUIDOrNull() ?: return@flow

		authenticationRepository.getUsersByServer(id)?.forEach { user -> emit(user) }
	}

	private fun getLegacyUsersForServer(server: Server): Flow<User> = flow {
		val user = credentialsFileSource.read()?.user ?: return@flow
		if (user.serverId.toUUID() == server.id.toUUID()) emit(user)
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun getServers(discovery: Boolean, stored: Boolean, legacy: Boolean): Flow<Server> = flow {
		if (discovery) emitAll(getDiscoveryServers())
		if (stored) emitAll(getStoredServers())
		if (legacy) emitAll(getLegacyServers())
	}.distinctUntilChangedBy { it.id }

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun getServersWithUsers(discovery: Boolean, stored: Boolean, legacy: Boolean): Flow<Pair<Server, List<User>>> = getServers(discovery, stored, legacy).map { server ->
		val users = flow {
			emitAll(getPublicUsersForServer(server))
			if (stored) emitAll(getStoredUsersForServer(server))
			if (legacy) emitAll(getLegacyUsersForServer(server))
		}.distinctUntilChangedBy { it.id.toUUID() }.toList()

		Pair(server, users)
	}

	override fun removeServer(serverId: UUID) {
		TODO("Not yet implemented")
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
