package org.jellyfin.androidtv.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.jellyfin.androidtv.JellyfinApplication
import org.jellyfin.androidtv.auth.model.*
import org.jellyfin.androidtv.util.apiclient.callApi
import org.jellyfin.androidtv.util.toUUID
import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.interaction.device.IDevice
import org.jellyfin.apiclient.model.dto.UserDto
import org.jellyfin.apiclient.model.users.AuthenticationResult
import timber.log.Timber
import java.util.*

class AuthenticationRepository(
	private val application: JellyfinApplication,
	private val jellyfin: Jellyfin,
	private val apiClient: ApiClient,
	private val device: IDevice,
	private val accountManagerHelper: AccountManagerHelper,
	private val authenticationStore: AuthenticationStore,
) {
	fun getServers() = authenticationStore.getServers().map { (id, info) ->
		Server(id, info.name, info.address, Date(info.lastUsed))
	}

	fun getUsers(server: UUID): List<User>? = authenticationStore.getUsers(server)?.mapNotNull { (userId, userInfo) ->
		accountManagerHelper.getAccount(userId)?.let { authInfo ->
			User(userId, authInfo.server, userInfo.name, authInfo.accessToken)
		}
	}

	fun saveServer(id: UUID, name: String, address: String) {
		val current = authenticationStore.getServer(id)

		if (current != null)
			authenticationStore.putServer(id, current.copy(name = name, address = address))
		else
			authenticationStore.putServer(id, AuthenticationStoreServer(name, address))
	}

	fun authenticateUser(user: UUID): Flow<LoginState> = flow {
		Timber.d("Authenticating user %s", user)
		emit(AuthenticatingState)

		val account = accountManagerHelper.getAccount(user)
		val server = account?.server?.let(authenticationStore::getServer)
		if (account?.accessToken != null && server != null) {
			apiClient.ChangeServerLocation(server.address)
			apiClient.SetAuthenticationInfo(account.accessToken, user.toString())

			try {
				val userDto = callApi<UserDto> { callback -> apiClient.GetUserAsync(user.toString(), callback) }
				application.currentUser = userDto

				if (application.currentUser == null) {
					emit(RequireSignInState)
				} else {
					emit(AuthenticatedState)
				}
			} catch (err: Exception) {
				// Assume it's a bad access token
				emit(RequireSignInState)
			}
		} else {
			// Failed
			emit(RequireSignInState)
		}
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	fun login(
		serverId: UUID,
		username: String,
		password: String
	) = flow {
		val server = authenticationStore.getServer(serverId)
			?: return@flow emit(ServerUnavailableState)

		val result = callApi<AuthenticationResult> { callback ->
			val api = jellyfin.createApi(server.address, device = device)
			api.AuthenticateUserAsync(username, password, callback)
		}

		val userId = result.user.id.toUUID()
		val currentUser = authenticationStore.getUser(serverId, userId)
		val updatedUser = currentUser?.copy(
			name = result.user.name,
			lastUsed = Date().time
		) ?: AuthenticationStoreUser(
			name = result.user.name,
		)

		authenticationStore.putUser(serverId, userId, updatedUser)
		accountManagerHelper.putAccount(AccountManagerAccount(userId, serverId, updatedUser.name, result.accessToken))

		emitAll(authenticateUser(userId))
	}
}

