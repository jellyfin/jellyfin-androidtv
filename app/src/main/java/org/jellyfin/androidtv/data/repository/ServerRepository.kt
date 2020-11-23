package org.jellyfin.androidtv.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.source.CredentialsFileSource
import org.jellyfin.androidtv.util.apiclient.callApi
import org.jellyfin.androidtv.util.apiclient.toServer
import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.interaction.device.IDevice
import org.jellyfin.apiclient.model.system.PublicSystemInfo
import timber.log.Timber

interface ServerRepository {
	fun getServers(): LiveData<List<Server>>

	fun discoverServers(): LiveData<List<Server>>

	suspend fun connect(address: String): Server
}

class ServerRepositoryImpl(
	private val jellyfin: Jellyfin,
	private val device: IDevice,
	private val credentialsFileSource: CredentialsFileSource,
	private val authenticationRepository: AuthenticationRepository
) : ServerRepository {
	override fun getServers() = liveData(Dispatchers.IO) {
		val servers = mutableListOf<Server>()

		servers += authenticationRepository.getServers()
		emit(servers as List<Server>)

		val legacyCredentials = credentialsFileSource.read()
		if (legacyCredentials?.server != null) {
			// Augment saved ServerInfo with PublicSystemInfo
			val api = jellyfin.createApi(serverAddress = legacyCredentials.server!!.address, device = device)
			val systemInfo: PublicSystemInfo = callApi { callback ->
				api.GetPublicSystemInfoAsync(callback)
			}
			servers += legacyCredentials.server!!.apply {
				name = systemInfo.serverName
				id = systemInfo.id
			}

			emit(servers as List<Server>)
		}
	}

	override fun discoverServers() = liveData(Dispatchers.IO) {
		val discoveredServers = mutableListOf<Server>()
		jellyfin.discovery.discover().collect { discoveredServer ->
			discoveredServers += discoveredServer.toServer()

			emit(discoveredServers as List<Server>)
		}
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
