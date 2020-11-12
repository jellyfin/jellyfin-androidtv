package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.source.CredentialsFileSource
import org.jellyfin.androidtv.util.apiclient.callApi
import org.jellyfin.androidtv.util.apiclient.toServer
import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.interaction.device.IDevice
import org.jellyfin.apiclient.model.system.PublicSystemInfo
import timber.log.Timber

interface ServerRepository {
	suspend fun getServers(): List<Server>

	suspend fun discoverServers(): List<Server>

	suspend fun connect(address: String): Server
}

class ServerRepositoryImpl(
	private val jellyfin: Jellyfin,
	private val device: IDevice,
	private val credentialsFileSource: CredentialsFileSource
) : ServerRepository {
	override suspend fun getServers(): List<Server> {
		val legacyCredentials = credentialsFileSource.read()
		if (legacyCredentials?.server != null) {
			// Augment saved ServerInfo with PublicSystemInfo
			val api = jellyfin.createApi(serverAddress = legacyCredentials.server!!.address, device = device)
			val systemInfo: PublicSystemInfo = callApi { callback ->
				api.GetPublicSystemInfoAsync(callback)
			}
			legacyCredentials.server!!.apply {
				name = systemInfo.serverName
				id = systemInfo.id
			}

			return listOf(legacyCredentials.server!!)
		}

		// TODO: Add new method of saving credentials

		return emptyList()
	}

	override suspend fun discoverServers() = withContext(Dispatchers.IO) {
		jellyfin.discovery.discover().toList().map { it.toServer() }
	}

	override suspend fun connect(address: String): Server {
		Timber.d("Creating api for server address %s", address)
		val api = jellyfin.createApi(serverAddress = address, device = device)
		val systemInfo: PublicSystemInfo = callApi { callback ->
			api.GetPublicSystemInfoAsync(callback)
		}
		return systemInfo.toServer().apply {
			// Use the entered address since SystemInfo can be wrong
			this.address = address
		}
	}
}
