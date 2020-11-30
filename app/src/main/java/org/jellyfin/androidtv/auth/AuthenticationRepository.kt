package org.jellyfin.androidtv.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.jellyfin.androidtv.JellyfinApplication
import org.jellyfin.androidtv.auth.model.AccountManagerAccount
import org.jellyfin.androidtv.auth.model.AuthenticationStoreServer
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.data.repository.*
import org.jellyfin.androidtv.data.source.CredentialsFileSource
import org.jellyfin.androidtv.util.apiclient.callApi
import org.jellyfin.androidtv.util.toUUID
import org.jellyfin.androidtv.util.toUUIDOrNull
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
	private val credentialsFileSource: CredentialsFileSource,
) {
	/**
	 * Remove accounts from authentication store that are not in the account manager.
	 * Should be run once on app start.
	 */
	fun sync() {
		val savedAccountIds = accountManagerHelper.getAccounts().map { it.id }

		authenticationStore.getServers().forEach { (serverId, server) ->
			server.users.forEach { (userId, _) ->
				if (!savedAccountIds.contains(userId))
					authenticationStore.removeUser(serverId, userId)
			}
		}
	}

	fun getServers() = authenticationStore.getServers().map { (id, info) ->
		Server(id.toString(), info.name, info.url, Date(info.lastUsed))
	}

	fun getUsers(): Map<Server, List<User>> = authenticationStore.getServers().map { (serverId, serverInfo) ->
		Server(serverId.toString(), serverInfo.name, serverInfo.url, Date(serverInfo.lastUsed)) to serverInfo.users.map { (userId, userInfo) ->
			val authInfo = accountManagerHelper.getAccount(userId)

			User(userId.toString(), userInfo.name, authInfo?.accessToken
				?: "", serverId.toString(), userInfo.profilePicture)
		}
	}.toMap()

	fun getUsersByServer(server: UUID): List<User>? = authenticationStore.getUsers(server)?.map { (userId, userInfo) ->
		val authInfo = accountManagerHelper.getAccount(userId)

		User(userId.toString(), userInfo.name, authInfo?.accessToken
			?: "", authInfo?.server.toString(), userInfo.profilePicture)
	}

	fun saveServer(id: UUID, name: String, address: String) {
		val current = authenticationStore.getServer(id)

		if (current != null)
			authenticationStore.putServer(id, current.copy(name = name, url = address))
		else
			authenticationStore.putServer(id, AuthenticationStoreServer(name, address))
	}

	fun authenticateUser(user: UUID): Flow<LoginState> = flow {
		Timber.d("Authenticating user %s", user)
		emit(AuthenticatingState)

		val account = accountManagerHelper.getAccount(user)
		val server = account?.server?.let(authenticationStore::getServer)
		if (account?.accessToken != null && server != null) {
			apiClient.ChangeServerLocation(server.url)
			apiClient.SetAuthenticationInfo(account.accessToken, user.toString())

			val userDto = callApi<UserDto> { callback -> apiClient.GetUserAsync(user.toString(), callback) }
			application.currentUser = userDto

			if (application.currentUser == null) {
				emit(RequireSignInState)
			} else {
				emit(AuthenticatedState)
			}
		} else {
			// TODO Try password-less login

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
		var server = authenticationStore.getServer(serverId)
		if (server == null) {
			val legacyCredentials = credentialsFileSource.read()
			if (legacyCredentials?.server?.id?.toUUIDOrNull() == serverId) {
				val serverInfo = legacyCredentials.server!!
				server = AuthenticationStoreServer(serverInfo.name, serverInfo.address, serverInfo.dateLastAccessed.time)
				authenticationStore.putServer(serverId, server)
			} else {
				return@flow emit(ServerUnavailableState)
			}
		}

		val result = callApi<AuthenticationResult> { callback ->
			val api = jellyfin.createApi(server.url, device = device)
			api.AuthenticateUserAsync(username, password, callback)
		}

		val userId = result.user.id.toUUID()
		val currentUser = authenticationStore.getUser(serverId, userId)
		val updatedUser = currentUser?.copy(
			name = result.user.name,
			profilePicture = result.user.primaryImageTag,
			lastUsed = Date().time
		) ?: AuthenticationStoreUser(
			name = result.user.name,
			profilePicture = result.user.primaryImageTag
		)

		authenticationStore.putUser(serverId, userId, updatedUser)
		accountManagerHelper.putAccount(AccountManagerAccount(userId, serverId, updatedUser.name, result.accessToken))

		emitAll(authenticateUser(userId))
	}
}

