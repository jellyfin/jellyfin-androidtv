package org.jellyfin.androidtv.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.jellyfin.androidtv.auth.model.*
import org.jellyfin.androidtv.util.sdk.toPublicUser
import org.jellyfin.androidtv.util.sdk.toServer
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.operations.UserApi
import org.jellyfin.sdk.discovery.RecommendedServerInfo
import org.jellyfin.sdk.discovery.RecommendedServerInfoScore
import org.jellyfin.sdk.model.api.ServerDiscoveryInfo
import org.jellyfin.sdk.model.api.UserDto
import org.jellyfin.sdk.model.serializer.toUUID
import timber.log.Timber
import java.util.*

interface ServerRepository {
	fun getStoredServers(): List<Server>
	fun getDiscoveryServers(): Flow<Server>
	suspend fun migrateLegacyCredentials()

	fun getServerUsers(server: Server): LiveData<List<User>>
	fun removeServer(serverId: UUID): Boolean
	fun addServer(address: String): Flow<ServerAdditionState>
}

class ServerRepositoryImpl(
	private val jellyfin: Jellyfin,
	private val authenticationRepository: AuthenticationRepository,
	private val authenticationStore: AuthenticationStore,
	private val legacyAccountMigration: LegacyAccountMigration
) : ServerRepository {
	override fun getStoredServers() = authenticationRepository.getServers()

	override fun getDiscoveryServers() = flow {
		val servers = jellyfin.discovery.discoverLocalServers().mapNotNull(ServerDiscoveryInfo::toServer)
		emitAll(servers)
	}.flowOn(Dispatchers.IO)

	private suspend fun getServerPublicUsers(server: Server): List<PublicUser> {
		val api = UserApi(jellyfin.createApi(server.address))
		val users by api.getPublicUsers()

		return users.mapNotNull(UserDto::toPublicUser)
	}

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

	override fun removeServer(serverId: UUID) = authenticationStore.removeServer(serverId)

	override fun addServer(address: String): Flow<ServerAdditionState> = flow {
		Timber.d("Adding server %s", address)

		emit(ConnectingState(address))

		val addressCandidates = jellyfin.discovery.getAddressCandidates(address)
		Timber.d("Found ${addressCandidates.size} candidates")

		val goodRecommendations = mutableListOf<RecommendedServerInfo>()
		val badRecommendations = mutableListOf<RecommendedServerInfo>()
		val greatRecommendaton = jellyfin.discovery.getRecommendedServers(addressCandidates)
			.firstOrNull { recommendedServer ->
				when (recommendedServer.score) {
					RecommendedServerInfoScore.GREAT -> true
					RecommendedServerInfoScore.GOOD -> {
						goodRecommendations += recommendedServer
						false
					}
					else -> {
						badRecommendations += recommendedServer
						false
					}
				}
			}

		Timber.d(buildString {
			append("Recommendations: ")
			if (greatRecommendaton == null) append(0)
			else append(1)
			append(" great, ")
			append(goodRecommendations.size)
			append(" good, ")
			append(badRecommendations.size)
			append(" bad")
		})

		val chosenRecommendation = greatRecommendaton ?: goodRecommendations.firstOrNull()
		if (chosenRecommendation != null) {
			val systemInfo = chosenRecommendation.systemInfo!!
			authenticationRepository.saveServer(
				id = systemInfo.id!!.toUUID(),
				name = systemInfo?.serverName ?: "Jellyfin Server",
				address = chosenRecommendation.address
			)

			emit(ConnectedState(systemInfo))
		} else {
			// No great or good recommendations, only add bad recommendations
			emit(UnableToConnectState(addressCandidates))
		}
	}
}
