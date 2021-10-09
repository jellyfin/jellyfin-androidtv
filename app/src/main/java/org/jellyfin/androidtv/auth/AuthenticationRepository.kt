package org.jellyfin.androidtv.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.jellyfin.androidtv.auth.model.*
import org.jellyfin.androidtv.preference.AuthenticationPreferences
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.apiclient.interaction.device.IDevice
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.KtorClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.operations.ImageApi
import org.jellyfin.sdk.api.operations.UserApi
import org.jellyfin.sdk.model.api.ImageType
import timber.log.Timber
import java.util.*

interface AuthenticationRepository {
	fun getServers(): List<Server>
	fun getUsers(server: UUID): List<PrivateUser>?
	fun getUser(server: UUID, user: UUID): PrivateUser?
	fun saveServer(id: UUID, name: String, address: String, version: String?, loginDisclaimer: String?)
	fun authenticateUser(user: User): Flow<LoginState>
	fun authenticateUser(user: User, server: Server): Flow<LoginState>
	fun login(server: Server, username: String, password: String = ""): Flow<LoginState>
	fun logout(user: User)
	fun removeUser(user: User)
	fun getUserImageUrl(server: Server, user: User): String?
}

class AuthenticationRepositoryImpl(
	private val jellyfin: Jellyfin,
	private val sessionRepository: SessionRepository,
	private val device: IDevice,
	private val accountManagerHelper: AccountManagerHelper,
	private val authenticationStore: AuthenticationStore,
	private val userApiClient: KtorClient,
	private val authenticationPreferences: AuthenticationPreferences,
) : AuthenticationRepository {
	private val serverComparator = compareByDescending<Server> { it.dateLastAccessed }.thenBy { it.name }
	private val userComparator = compareByDescending<PrivateUser> { it.lastUsed }.thenBy { it.name }

	override fun getServers() = authenticationStore.getServers().map { (id, info) ->
		Server(
			id = id,
			name = info.name,
			address = info.address,
			version = info.version,
			loginDisclaimer = info.loginDisclaimer,
			dateLastAccessed = Date(info.lastUsed),
		)
	}.sortedWith(serverComparator)

	private fun mapUser(
		serverId: UUID,
		userId: UUID,
		userInfo: AuthenticationStoreUser,
		authInfo: AccountManagerAccount?,
	) = PrivateUser(
		id = authInfo?.id ?: userId,
		serverId = authInfo?.server ?: serverId,
		name = userInfo.name,
		accessToken = authInfo?.accessToken,
		requirePassword = userInfo.requirePassword,
		imageTag = userInfo.imageTag,
		lastUsed = userInfo.lastUsed,
	)

	override fun getUsers(server: UUID): List<PrivateUser>? =
		authenticationStore.getUsers(server)?.mapNotNull { (userId, userInfo) ->
			accountManagerHelper.getAccount(userId).let { authInfo ->
				mapUser(server, userId, userInfo, authInfo)
			}
		}?.sortedWith(userComparator)

	override fun getUser(server: UUID, user: UUID): PrivateUser? {
		val userInfo = authenticationStore.getUser(server, user) ?: return null
		val authInfo = accountManagerHelper.getAccount(user) ?: return null

		return mapUser(server, user, userInfo, authInfo)
	}

	override fun saveServer(id: UUID, name: String, address: String, version: String?, loginDisclaimer: String?) {
		val current = authenticationStore.getServer(id)
		val server = if (current != null) current.copy(name = name, address = address, version = version, loginDisclaimer = loginDisclaimer, lastUsed = Date().time)
		else AuthenticationStoreServer(name, address, version, loginDisclaimer)

		authenticationStore.putServer(id, server)
	}

	/**
	 * Set the active session to the information in [user] and [server].
	 * Connects to the server and requests the info of the currently authenticated user.
	 *
	 * @return Whether the user information can be retrieved.
	 */
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

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun authenticateUser(user: User): Flow<LoginState> = flow {
		Timber.d("Authenticating serverless user %s", user)
		emit(AuthenticatingState)

		val server = authenticationStore.getServer(user.serverId)?.let {
			Server(
				id = user.serverId,
				name = it.name,
				address = it.address,
				version = it.version,
				loginDisclaimer = it.loginDisclaimer,
				dateLastAccessed = Date(it.lastUsed),
			)
		}

		if (server == null) emit(RequireSignInState)
		else if (!server.versionSupported) emit(ServerVersionNotSupported(server))
		else emitAll(authenticateUser(user, server))
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun authenticateUser(user: User, server: Server): Flow<LoginState> = flow {
		Timber.d("Authenticating user %s", user)
		emit(AuthenticatingState)

		val account = accountManagerHelper.getAccount(user.id)
		when {
			!server.versionSupported -> emit(ServerVersionNotSupported(server))
			// Access token found, proceed with sign in
			!authenticationPreferences[AuthenticationPreferences.alwaysAuthenticate] && account?.accessToken != null -> when {
				// Update session
				setActiveSession(user, server) -> {
					// Update stored user
					try {
						val userInfo by UserApi(userApiClient).getCurrentUser()
						val currentUser = authenticationStore.getUser(server.id, user.id)

						val updatedUser = currentUser?.copy(
							name = userInfo.name!!,
							requirePassword = userInfo.hasPassword,
							imageTag = userInfo.primaryImageTag
						) ?: AuthenticationStoreUser(
							name = userInfo.name!!,
							requirePassword = userInfo.hasPassword,
							imageTag = userInfo.primaryImageTag
						)
						authenticationStore.putUser(server.id, user.id, updatedUser)
						accountManagerHelper.putAccount(AccountManagerAccount(user.id, server.id, updatedUser.name, account.accessToken))

						emit(AuthenticatedState)
					} catch(err: ApiClientException) {
						Timber.e(err, "Unable to get current user data")
						emit(RequireSignInState)
					}
				}
				// Login failed
				else -> when {
					// No password required - try login
					!user.requirePassword -> emitAll(login(server, user.name))
					// Require new sign in
					else -> emit(RequireSignInState)
				}
			}
			// User is known to not require a password, try a sign in
			!user.requirePassword -> emitAll(login(server, user.name))
			// Account found without access token, require sign in
			else -> emit(RequireSignInState)
		}
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun login(server: Server, username: String, password: String) = flow {
		if (!server.versionSupported) {
			emit(ServerVersionNotSupported(server))
			return@flow
		}

		val api = jellyfin.createApi(server.address, deviceInfo = AuthenticationDevice(device, username).toDeviceInfo())
		val userApi = UserApi(api)
		val result = try {
			val response = userApi.authenticateUserByName(username, password)
			response.content
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to sign in as $username")
			emit(RequireSignInState)
			return@flow
		}

		val userInfo = result.user ?: return@flow emit(RequireSignInState)

		val userId = userInfo.id
		val currentUser = authenticationStore.getUser(server.id, userId)
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

		authenticationStore.putUser(server.id, userId, updatedUser)
		val accountManagerAccount = AccountManagerAccount(userId, server.id, updatedUser.name, result.accessToken)
		accountManagerHelper.putAccount(accountManagerAccount)

		val user = PrivateUser(
			id = userId,
			serverId = server.id,
			name = updatedUser.name,
			accessToken = result.accessToken,
			requirePassword = userInfo.hasPassword,
			imageTag = userInfo.primaryImageTag,
			lastUsed = Date().time,
		)

		// We just added the account so it should activate properly although in rare cases it doesn't.
		// this is often caused by issues in the platforms account manager
		if (setActiveSession(user, server)) emit(AuthenticatedState)
		else {
			// Try to remove the account and ask for sign in
			accountManagerHelper.removeAccount(accountManagerAccount)
			emit(RequireSignInState)
		}
	}

	override fun logout(user: User) {
		accountManagerHelper.getAccount(user.id)?.let(accountManagerHelper::removeAccount)
	}

	override fun removeUser(user: User) {
		authenticationStore.removeUser(user.serverId, user.id)
		logout(user)
	}

	override fun getUserImageUrl(server: Server, user: User): String? {
		val api = jellyfin.createApi(baseUrl = server.address)
		val imageApi = ImageApi(api)

		return user.imageTag?.let { imageTag ->
			imageApi.getUserImageUrl(
				userId = user.id,
				tag = imageTag,
				imageType = ImageType.PRIMARY,
				maxHeight = ImageUtils.MAX_PRIMARY_IMAGE_HEIGHT
			)
		}
	}
}
