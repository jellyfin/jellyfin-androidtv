package org.jellyfin.androidtv.auth.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import org.jellyfin.androidtv.auth.model.AuthenticationStoreServer
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.ConnectingState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.ServerAdditionState
import org.jellyfin.androidtv.auth.model.UnableToConnectState
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import org.jellyfin.androidtv.util.sdk.toServer
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.brandingApi
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.discovery.RecommendedServerInfo
import org.jellyfin.sdk.discovery.RecommendedServerInfoScore
import org.jellyfin.sdk.model.api.ServerDiscoveryInfo
import org.jellyfin.sdk.model.serializer.toUUID
import timber.log.Timber
import java.util.Date
import java.util.UUID

/**
 * Repository to maintain servers.
 */
interface ServerRepository {
	val storedServers: StateFlow<List<Server>>
	val discoveredServers: StateFlow<List<Server>>

	suspend fun loadStoredServers()
	suspend fun loadDiscoveryServers()

	fun addServer(address: String): Flow<ServerAdditionState>
	suspend fun updateServer(server: Server): Boolean
	suspend fun deleteServer(server: UUID): Boolean

	companion object {
		val minimumServerVersion = Jellyfin.minimumVersion.copy(build = null)
	}
}

class ServerRepositoryImpl(
	private val jellyfin: Jellyfin,
	private val authenticationStore: AuthenticationStore,
) : ServerRepository {
	// State
	private val _storedServers = MutableStateFlow(emptyList<Server>())
	override val storedServers = _storedServers.asStateFlow()

	private val _discoveredServers = MutableStateFlow(emptyList<Server>())
	override val discoveredServers = _discoveredServers.asStateFlow()

	// Loading data
	override suspend fun loadStoredServers() {
		authenticationStore.getServers()
			.map { (id, entry) -> entry.asServer(id) }
			.sortedWith(compareByDescending<Server> { it.dateLastAccessed }.thenBy { it.name })
			.let { _storedServers.emit(it) }
	}

	override suspend fun loadDiscoveryServers() {
		val servers = mutableListOf<Server>()

		jellyfin.discovery
			.discoverLocalServers()
			.mapNotNull(ServerDiscoveryInfo::toServer)
			.collect { server ->
				servers += server
				_discoveredServers.emit(servers.toList())
			}
	}

	// Mutating data
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

			val server = authenticationStore.getServer(id)?.copy(
				name = systemInfo.serverName ?: "Jellyfin Server",
				address = chosenRecommendation.address,
				version = systemInfo.version,
				loginDisclaimer = branding.loginDisclaimer,
				lastUsed = Date().time
			) ?: AuthenticationStoreServer(
				name = systemInfo.serverName ?: "Jellyfin Server",
				address = chosenRecommendation.address,
				version = systemInfo.version,
				loginDisclaimer = branding.loginDisclaimer
			)

			authenticationStore.putServer(id, server)
			loadStoredServers()

			emit(ConnectedState(id, systemInfo))
		} else {
			// No great or good recommendations, only add bad recommendations
			val addressCandidatesWithIssues = (badRecommendations + goodRecommendations)
				.groupBy { it.address }
				.mapValues { (_, entry) -> entry.flatMap { server -> server.issues } }
			emit(UnableToConnectState(addressCandidatesWithIssues))
		}
	}

	override suspend fun updateServer(server: Server): Boolean {
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

	override suspend fun deleteServer(server: UUID): Boolean {
		val success = authenticationStore.removeServer(server)
		if (success) loadStoredServers()
		return success
	}

	// Helper functions
	private fun AuthenticationStoreServer.asServer(id: UUID) = Server(
		id = id,
		name = name,
		address = address,
		version = version,
		loginDisclaimer = loginDisclaimer,
		dateLastAccessed = Date(lastUsed),
	)
}
