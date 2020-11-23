package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.data.model.LogonCredentials
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.data.source.CredentialsFileSource
import org.jellyfin.androidtv.util.apiclient.callApi
import org.jellyfin.androidtv.util.apiclient.getPublicUsers
import org.jellyfin.androidtv.util.apiclient.toServerInfo
import org.jellyfin.androidtv.util.apiclient.toUser
import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.interaction.device.IDevice
import org.jellyfin.apiclient.model.users.AuthenticationResult

interface UserRepository {
	suspend fun getUsers(server: Server): List<User>

	suspend fun login(server: Server, username: String, password: String): AuthenticationResult
}

class UserRepositoryImpl(
	private val jellyfin: Jellyfin,
	private val apiClient: ApiClient,
	private val device: IDevice,
	private val credentialsFileSource: CredentialsFileSource,
	private val authenticationRepository: AuthenticationRepository
) : UserRepository {
	override suspend fun getUsers(server: Server): List<User> = withContext(Dispatchers.IO) {
		val api = jellyfin.createApi(serverAddress = server.address, device = device)
		api.getPublicUsers()?.toList()?.map { it.toUser() } ?: emptyList()
	}

	override suspend fun login(server: Server, username: String, password: String): AuthenticationResult {
		authenticationRepository.login(server, username, password)

		// TODO: Are both of these updates needed?
		// Update the server address the apiClient uses
		apiClient.EnableAutomaticNetworking(server.toServerInfo())
		apiClient.ChangeServerLocation(server.address)

		// Authenticate using given credentials
		val result: AuthenticationResult = callApi { callback ->
			apiClient.AuthenticateUserAsync(username, password, callback)
		}

		// Save the current login information
		credentialsFileSource.write(LogonCredentials(server, result.user.toUser()))

		return result
	}
}
