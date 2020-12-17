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
import org.jellyfin.apiclient.model.apiclient.ServerInfo
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

	fun getUsers(server: UUID): List<PrivateUser>? = authenticationStore.getUsers(server)?.mapNotNull { (userId, userInfo) ->
		accountManagerHelper.getAccount(userId)?.let { authInfo ->
			PrivateUser(userId, authInfo.server, userInfo.name, authInfo.accessToken)
		}
	}

	fun saveServer(id: UUID, name: String, address: String) {
		val current = authenticationStore.getServer(id)

		if (current != null)
			authenticationStore.putServer(id, current.copy(name = name, address = address))
		else
			authenticationStore.putServer(id, AuthenticationStoreServer(name, address))
	}

	/**
	 * Set the active session to the information in [user] and [server].
	 * Connects to the server and requests the info of the currently authenticated user.
	 *
	 * @return Whether the user information can be retrieved
	 */
	private suspend fun setActiveSession(user: User, server: Server): Boolean {
		apiClient.SetAuthenticationInfo(user.accessToken, user.id.toString())
		apiClient.EnableAutomaticNetworking(ServerInfo().apply {
			id = server.id.toString()
			name = server.name
			address = server.address
			userId = user.id.toString()
			accessToken = user.accessToken
		})

		try {
			val userDto = callApi<UserDto?> { callback -> apiClient.GetUserAsync(user.id.toString(), callback) }
			if (userDto != null) {
				application.currentUser = userDto
				return true
			}
		} catch (err: Exception) {
			Timber.e(err, "Could not get user information while access token was set")
		}

		return false
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	fun authenticateUser(user: User): Flow<LoginState> = flow {
		Timber.d("Authenticating serverless user %s", user)
		emit(AuthenticatingState)

		val server = authenticationStore.getServer(user.serverId)?.let {
			Server(user.serverId, it.name, it.address, Date(it.lastUsed))
		}

		if (server == null) emit(RequireSignInState)
		else emitAll(authenticateUser(user, server))
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	fun authenticateUser(user: User, server: Server): Flow<LoginState> = flow {
		Timber.d("Authenticating user %s", user)
		emit(AuthenticatingState)

		val account = accountManagerHelper.getAccount(user.id)
		when {
			// Access token found, proceed with sign in
			account?.accessToken != null -> {
				val authenticated = setActiveSession(user, server)
				if (authenticated) emit(AuthenticatedState)
				else emit(RequireSignInState)
			}
			// User is known to not require a password, try a sign in
			user is PublicUser && !user.requirePassword -> emitAll(login(server.id, user.name))
			// Account found without access token, require sign in
			else -> emit(RequireSignInState)
		}
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	fun login(
		serverId: UUID,
		username: String,
		password: String = ""
	) = flow {
		val server = authenticationStore.getServer(serverId)?.let {
			Server(serverId, it.name, it.address, Date(it.lastUsed))
		} ?: return@flow emit(ServerUnavailableState)

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

		val user = PrivateUser(userId, serverId, updatedUser.name, result.accessToken)
		val authenticated = setActiveSession(user, server)
		if (authenticated) emit(AuthenticatedState)
		else emit(RequireSignInState)
	}
}

