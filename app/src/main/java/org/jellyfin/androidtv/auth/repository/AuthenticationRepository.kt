package org.jellyfin.androidtv.auth.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.jellyfin.androidtv.auth.model.AccountManagerAccount
import org.jellyfin.androidtv.auth.model.AuthenticateMethod
import org.jellyfin.androidtv.auth.model.AuthenticatedState
import org.jellyfin.androidtv.auth.model.AuthenticatingState
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.androidtv.auth.model.AutomaticAuthenticateMethod
import org.jellyfin.androidtv.auth.model.CredentialAuthenticateMethod
import org.jellyfin.androidtv.auth.model.LoginState
import org.jellyfin.androidtv.auth.model.PrivateUser
import org.jellyfin.androidtv.auth.model.RequireSignInState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.ServerVersionNotSupported
import org.jellyfin.androidtv.auth.model.User
import org.jellyfin.androidtv.auth.store.AccountManagerStore
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.sdk.forUser
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.UserDto
import timber.log.Timber
import java.util.Date

/**
 * Repository to manage authentication of the user in the app.
 */
interface AuthenticationRepository {
	fun authenticate(server: Server, method: AuthenticateMethod): Flow<LoginState>
	fun logout(user: User): Boolean
	fun getUserImageUrl(server: Server, user: User): String?
}

class AuthenticationRepositoryImpl(
	private val jellyfin: Jellyfin,
	private val sessionRepository: SessionRepository,
	private val accountManagerStore: AccountManagerStore,
	private val authenticationStore: AuthenticationStore,
	private val userApiClient: ApiClient,
	private val authenticationPreferences: AuthenticationPreferences,
	private val defaultDeviceInfo: DeviceInfo,
) : AuthenticationRepository {
	override fun authenticate(server: Server, method: AuthenticateMethod): Flow<LoginState> {
		// Check server version first
		if (!server.versionSupported) return flowOf(ServerVersionNotSupported(server))

		return when (method) {
			is AutomaticAuthenticateMethod -> authenticateAutomatic(server, method.user)
			is CredentialAuthenticateMethod -> authenticateCredential(server, method.username, method.password)
		}
	}

	private fun authenticateAutomatic(server: Server, user: User): Flow<LoginState> {
		Timber.d("Authenticating user %s", user)

		// Automatic logic is disabled when the always authenticate preference is enabled
		if (authenticationPreferences[AuthenticationPreferences.alwaysAuthenticate]) return flowOf(RequireSignInState)

		val account = accountManagerStore.getAccount(user.id)
		// Try login with access token
		return if (account?.accessToken != null) authenticateToken(server, user.withToken(account.accessToken))
		// Try login without password
		else if (!user.requirePassword) authenticateCredential(server, user.name, "")
		// Require login
		else flowOf(RequireSignInState)
	}

	private fun authenticateCredential(server: Server, username: String, password: String) = flow {
		val api = jellyfin.createApi(server.address, deviceInfo = defaultDeviceInfo.forUser(username))
		val result = try {
			val response = api.userApi.authenticateUserByName(username, password)
			response.content
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to sign in as $username")
			emit(RequireSignInState)
			return@flow
		}

		val accessToken = result.accessToken ?:return@flow emit(RequireSignInState)
		val userInfo = result.user ?: return@flow emit(RequireSignInState)
		val user = PrivateUser(
			id = userInfo.id,
			serverId = server.id,
			name = userInfo.name!!,
			accessToken = result.accessToken,
			requirePassword = userInfo.hasPassword,
			imageTag = userInfo.primaryImageTag,
			lastUsed = Date().time,
		)

		authenticateFinish(server, userInfo, accessToken)
		val success = setActiveSession(user, server)
		if (success) emit(AuthenticatedState)
		else emit(RequireSignInState)
	}

	private fun authenticateToken(server: Server, user: User) = flow {
		emit(AuthenticatingState)

		val success = setActiveSession(user, server)
		if (!success) {
			emit(RequireSignInState)
		} else try {
			// Update user info
			val userInfo by userApiClient.userApi.getCurrentUser()
			authenticateFinish(server, userInfo, user.accessToken.orEmpty())
			emit(AuthenticatedState)
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to get current user data")
			emit(RequireSignInState)
		}
	}

	private suspend fun authenticateFinish(server: Server, userInfo: UserDto, accessToken: String) {
		val currentUser = authenticationStore.getUser(server.id, userInfo.id)

		val updatedUser = currentUser?.copy(
			name = userInfo.name!!,
			lastUsed = Date().time,
			requirePassword = userInfo.hasPassword,
			imageTag = userInfo.primaryImageTag
		) ?: AuthenticationStoreUser(
			name = userInfo.name!!,
			requirePassword = userInfo.hasPassword,
			imageTag = userInfo.primaryImageTag
		)
		authenticationStore.putUser(server.id, userInfo.id, updatedUser)

		val accountManagerAccount = AccountManagerAccount(userInfo.id, server.id, updatedUser.name, accessToken)
		accountManagerStore.putAccount(accountManagerAccount)
	}

	private suspend fun setActiveSession(user: User, server: Server): Boolean {
		val authenticated = sessionRepository.switchCurrentSession(user.id)

		if (authenticated) {
			// Update last use in store
			authenticationStore.getServer(server.id)?.let { storedServer ->
				authenticationStore.putServer(server.id, storedServer.copy(lastUsed = Date().time))
			}

			authenticationStore.getUser(server.id, user.id)?.let { storedUser ->
				authenticationStore.putUser(server.id, user.id, storedUser.copy(lastUsed = Date().time))
			}
		}

		return authenticated
	}

	override fun logout(user: User): Boolean {
		val authInfo = accountManagerStore.getAccount(user.id) ?: return false
		return accountManagerStore.removeAccount(authInfo)
	}

	override fun getUserImageUrl(server: Server, user: User): String? = user.imageTag?.let { tag ->
		jellyfin.createApi(server.address).imageApi.getUserImageUrl(
			userId = user.id,
			tag = tag,
			imageType = ImageType.PRIMARY,
			maxHeight = ImageUtils.MAX_PRIMARY_IMAGE_HEIGHT
		)
	}
}
