package org.jellyfin.androidtv.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.ConnectingState
import org.jellyfin.androidtv.auth.model.PrivateUser
import org.jellyfin.androidtv.auth.model.PublicUser
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.ServerAdditionState
import org.jellyfin.androidtv.auth.model.UnableToConnectState
import org.jellyfin.androidtv.auth.model.User
import org.jellyfin.androidtv.util.sdk.toPublicUser
import org.jellyfin.androidtv.util.sdk.toServer
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.brandingApi
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.discovery.RecommendedServerInfo
import org.jellyfin.sdk.discovery.RecommendedServerInfoScore
import org.jellyfin.sdk.model.api.ServerDiscoveryInfo
import org.jellyfin.sdk.model.api.UserDto
import org.jellyfin.sdk.model.serializer.toUUID
import timber.log.Timber
import java.util.Date
import java.util.UUID

interface ServerRepository {
	fun getStoredServers(): List<Server>
	fun getDiscoveryServers(): Flow<Server>
	suspend fun migrateLegacyCredentials()

	fun getServerUsers(server: Server): LiveData<List<User>>
	fun removeServer(serverId: UUID): Boolean
	fun addServer(address: String): Flow<ServerAdditionState>
	suspend fun refreshServerInfo(server: Server): Boolean

	companion object {
		val minimumServerVersion = Jellyfin.minimumVersion.copy(build = null)
	}
}

class ServerRepositoryImpl(
	private val jellyfin: Jellyfin,
	private val authenticationRepository: AuthenticationRepository,
	private val authenticationStore: AuthenticationStore,
	private val legacyAccountMigration: LegacyAccountMigration,
) : ServerRepository {
	override fun getStoredServers() = authenticationRepository.getServers()

	override fun getDiscoveryServers() = flow {
		val servers = jellyfin.discovery.discoverLocalServers().mapNotNull(ServerDiscoveryInfo::toServer)
		emitAll(servers)
	}.flowOn(Dispatchers.IO)

	private suspend fun getServerPublicUsers(server: Server): List<PublicUser> {
		val api = jellyfin.createApi(server.address)

		return try {
			val users by api.userApi.getPublicUsers()
			users.mapNotNull(UserDto::toPublicUser)
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to retrieve public users")

			emptyList()
		}
	}

	public override suspend fun refreshServerInfo(server: Server): Boolean {
		// Only update existing servers
		val serverInfo = authenticationStore.getServer(server.id) ?: return false
		val now = Date().time

		// Only update every 10 minutes
		if (now - serverInfo.lastRefreshed < 600000 && serverInfo.version != null) return false

		return try {
			val api = jellyfin.createApi(server.address)
			// Get login disclaimer
			val branding by api.brandingApi.getBrandingOptions()
			val systemInfo by api.systemApi.getPublicSystemInfo()

			authenticationStore.putServer(server.id, serverInfo.copy(
				name = systemInfo.serverName ?: serverInfo.name,
				version = systemInfo.version ?: serverInfo.version,
				loginDisclaimer = branding.loginDisclaimer ?: serverInfo.loginDisclaimer,
				lastRefreshed = now
			))
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to update server")

			false
		}
	}

	private fun getServerStoredUsers(server: Server): List<PrivateUser> = authenticationRepository
		.getUsers(server.id)
		.orEmpty()

	override fun getServerUsers(server: Server): LiveData<List<User>> = liveData {
		val users = mutableListOf<User>()

		users.addAll(getServerStoredUsers(server))
		emit(users.toList())

		getServerPublicUsers(server)
			.sortedBy { it.name }
			.forEach { user ->
				val index = users.indexOfFirst { it.id == user.id }
				if (index == -1) users.add(user)
				else {
					users[index] = when (val currentUser = users[index]) {
						is PublicUser -> user
						is PrivateUser -> currentUser.copy(
							name = user.name,
							requirePassword = user.requirePassword,
							imageTag = user.imageTag
						)
					}
				}
			}

		emit(users.toList())
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
		val greatRecommendaton = jellyfin.discovery.getRecommendedServers(addressCandidates).firstOrNull { recommendedServer ->
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
		if (chosenRecommendation != null && chosenRecommendation.systemInfo.isSuccess) {
			// Get system info
			val systemInfo = chosenRecommendation.systemInfo.getOrThrow()

			// Get branding info
			val api = jellyfin.createApi(chosenRecommendation.address).brandingApi
			val branding by api.getBrandingOptions()

			val id = systemInfo.id!!.toUUID()
			authenticationRepository.saveServer(
				id = id,
				name = systemInfo.serverName ?: "Jellyfin Server",
				address = chosenRecommendation.address,
				version = systemInfo.version,
				loginDisclaimer = branding.loginDisclaimer,
			)

			emit(ConnectedState(id, systemInfo))
		} else {
			// No great or good recommendations, only add bad recommendations
			val addressCandidatesWithIssues = (badRecommendations + goodRecommendations)
				.groupBy { it.address }
				.mapValues { (_, entry) -> entry.flatMap { server -> server.issues } }
			emit(UnableToConnectState(addressCandidatesWithIssues))
		}
	}
}
