package org.jellyfin.androidtv.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
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
import org.jellyfin.apiclient.model.dto.UserDto
import org.jellyfin.apiclient.model.system.PublicSystemInfo
import timber.log.Timber
import java.util.*

interface ServerRepository {
	fun getStoredServers(): List<Server>
	fun getDiscoveryServers(): Flow<Server>
	suspend fun migrateLegacyCredentials()

	fun getServerUsers(server: Server): LiveData<List<User>>
	fun removeServer(serverId: UUID): Unit
	fun addServer(address: String): Flow<ServerAdditionState>
}

class ServerRepositoryImpl(
	private val jellyfin: Jellyfin,
	private val device: IDevice,
	private val authenticationRepository: AuthenticationRepository,
	private val authenticationStore: AuthenticationStore,
	private val legacyAccountMigration: LegacyAccountMigration
) : ServerRepository {
	override fun getStoredServers() = authenticationRepository.getServers()

	override fun getDiscoveryServers() = flow {
		val servers = jellyfin.discovery.discover().map(DiscoveryServerInfo::toServer)
		emitAll(servers)
	}.flowOn(Dispatchers.IO)

	private suspend fun getServerPublicUsers(server: Server): List<PublicUser> = jellyfin
		.createApi(server.address, device = device)
		.getPublicUsers()
		?.toList()
		.orEmpty()
		.map(UserDto::toPublicUser)

	private fun getServerStoredUsers(server: Server): List<PrivateUser> = authenticationRepository
		.getUsers(server.id)
		.orEmpty()

	override fun getServerUsers(server: Server): LiveData<List<User>> = liveData {
		val users = mutableListOf<User>()

		users.addAll(getServerStoredUsers(server))
		emit(users)

		getServerPublicUsers(server)
			.sortedBy { it.name }
			.forEach { user ->
				if (users.none { it.id == user.id })
					users.add(user)
			}

		emit(users)
	}

	override suspend fun migrateLegacyCredentials() = legacyAccountMigration.migrate()

	override fun removeServer(serverId: UUID) {
		authenticationStore.removeServer(serverId)
	}

	override fun addServer(address: String): Flow<ServerAdditionState> = flow {
		Timber.d("Adding server %s", address)

		emit(ConnectingState)

		// Suppressed because the old apiclient is unreliable
		@Suppress("TooGenericExceptionCaught")
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
