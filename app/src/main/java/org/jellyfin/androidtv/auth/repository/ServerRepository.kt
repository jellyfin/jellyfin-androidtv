package org.jellyfin.androidtv.auth.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.model.AuthenticationStoreServer
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.ConnectingState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.ServerAdditionState
import org.jellyfin.androidtv.auth.model.UnableToConnectState
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import org.jellyfin.androidtv.util.sdk.toServer
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidContentException
import org.jellyfin.sdk.api.client.extensions.brandingApi
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.discovery.RecommendedServerInfo
import org.jellyfin.sdk.discovery.RecommendedServerInfoScore
import org.jellyfin.sdk.model.ServerVersion
import org.jellyfin.sdk.model.api.ServerDiscoveryInfo
import org.jellyfin.sdk.model.serializer.toUUID
import timber.log.Timber
import java.time.Instant
import java.util.UUID
import org.jellyfin.sdk.model.api.BrandingOptions as BrandingOptionsDto

/**
 * Repository to maintain servers.
 */
interface ServerRepository {
	val storedServers: StateFlow<List<Server>>
	val discoveredServers: StateFlow<List<Server>>
	val currentServer: StateFlow<Server?>

	suspend fun loadStoredServers()
	suspend fun loadDiscoveryServers()

	fun setCurrentServer(server: Server?)

	fun addServer(address: String): Flow<ServerAdditionState>
	suspend fun getServer(id: UUID, eagerUpdate: Boolean = false): Server?
	suspend fun updateServer(server: Server, force: Boolean = false): Boolean
	suspend fun deleteServer(server: UUID): Boolean

	companion object {
		val minimumServerVersion = Jellyfin.minimumVersion.copy(build = null)
		val recommendedServerVersion = Jellyfin.apiVersion.copy(build = null)

		val upcomingMinimumServerVersion = ServerVersion(10, 10, 0)
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

	private val _currentServer = MutableStateFlow<Server?>(null)
	override val currentServer = _currentServer.asStateFlow()

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
			.map(ServerDiscoveryInfo::toServer)
			.collect { server ->
				servers += server
				_discoveredServers.emit(servers.toList())
			}
	}

	override fun setCurrentServer(server: Server?) {
		_currentServer.value = server
	}

	// Mutating data
	override fun addServer(address: String): Flow<ServerAdditionState> = flow {
		Timber.i("Adding server %s", address)

		emit(ConnectingState(address))

		val addressCandidates = jellyfin.discovery.getAddressCandidates(address)
		Timber.i("Found ${addressCandidates.size} candidates")

		val goodRecommendations = mutableListOf<RecommendedServerInfo>()
		val badRecommendations = mutableListOf<RecommendedServerInfo>()
		val greatRecommendation = jellyfin.discovery.getRecommendedServers(addressCandidates).firstOrNull { recommendedServer ->
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

		Timber.i(buildString {
			append("Recommendations: ")
			if (greatRecommendation == null) append(0)
			else append(1)
			append(" great, ")
			append(goodRecommendations.size)
			append(" good, ")
			append(badRecommendations.size)
			append(" bad")
		})

		val chosenRecommendation = greatRecommendation ?: goodRecommendations.firstOrNull()
		if (chosenRecommendation != null && chosenRecommendation.systemInfo.isSuccess) {
			// Get system info
			val systemInfo = chosenRecommendation.systemInfo.getOrThrow()

			// Get branding info
			val api = jellyfin.createApi(chosenRecommendation.address)
			val branding = api.getBrandingOptionsOrDefault()

			val id = systemInfo.id!!.toUUID()

			val server = authenticationStore.getServer(id)?.copy(
				name = systemInfo.serverName ?: "Jellyfin Server",
				address = chosenRecommendation.address,
				version = systemInfo.version,
				loginDisclaimer = branding.loginDisclaimer,
				splashscreenEnabled = branding.splashscreenEnabled,
				setupCompleted = systemInfo.startupWizardCompleted ?: true,
				lastUsed = Instant.now().toEpochMilli()
			) ?: AuthenticationStoreServer(
				name = systemInfo.serverName ?: "Jellyfin Server",
				address = chosenRecommendation.address,
				version = systemInfo.version,
				loginDisclaimer = branding.loginDisclaimer,
				splashscreenEnabled = branding.splashscreenEnabled,
				setupCompleted = systemInfo.startupWizardCompleted ?: true,
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
	}.flowOn(Dispatchers.IO)

	override suspend fun getServer(id: UUID, eagerUpdate: Boolean): Server? {
		val server = authenticationStore.getServer(id) ?: return null

		val updatedServer = try {
			val forceUpdate = eagerUpdate && server.version
				?.let(ServerVersion::fromString)
				?.let { version -> version < ServerRepository.minimumServerVersion } == true

			updateServerInternal(id, server, forceUpdate)
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to update server")
			null
		}

		return (updatedServer ?: server).asServer(id)
	}

	override suspend fun updateServer(server: Server, force: Boolean): Boolean {
		// Only update existing servers
		val serverInfo = authenticationStore.getServer(server.id) ?: return false

		return try {
			updateServerInternal(server.id, serverInfo, force) != null
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to update server")

			false
		}
	}

	private suspend fun updateServerInternal(
		id: UUID,
		server: AuthenticationStoreServer,
		forceUpdate: Boolean
	): AuthenticationStoreServer? {
		val now = Instant.now().toEpochMilli()

		// Only update every 10 minutes
		if (now - server.lastRefreshed < 600000 && server.version != null && !forceUpdate) return null

		val newServer = withContext(Dispatchers.IO) {
			val api = jellyfin.createApi(server.address)

			// Get login disclaimer
			val branding = api.getBrandingOptionsOrDefault()
			val systemInfo by api.systemApi.getPublicSystemInfo()

			server.copy(
				name = systemInfo.serverName ?: server.name,
				version = systemInfo.version ?: server.version,
				loginDisclaimer = branding.loginDisclaimer ?: server.loginDisclaimer,
				splashscreenEnabled = branding.splashscreenEnabled,
				setupCompleted = systemInfo.startupWizardCompleted ?: server.setupCompleted,
				lastRefreshed = now
			)
		}
		authenticationStore.putServer(id, newServer)

		return newServer
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
		splashscreenEnabled = splashscreenEnabled,
		setupCompleted = setupCompleted,
		dateLastAccessed = Instant.ofEpochMilli(lastUsed),
	)

	/**
	 * Try to retrieve the branding options. If the response JSON is invalid it will return a default value.
	 * This makes sure we can still work with older Jellyfin versions.
	 */
	private suspend fun ApiClient.getBrandingOptionsOrDefault() = try {
		brandingApi.getBrandingOptions().content
	} catch (exception: InvalidContentException) {
		Timber.w(exception, "Invalid branding options response, using default value")
		BrandingOptionsDto(
			loginDisclaimer = null,
			customCss = null,
			splashscreenEnabled = false,
		)
	}
}
