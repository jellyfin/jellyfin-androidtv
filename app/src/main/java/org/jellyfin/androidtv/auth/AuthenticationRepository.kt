package org.jellyfin.androidtv.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.jellyfin.androidtv.auth.model.*
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.apiclient.callApi
import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.interaction.device.IDevice
import org.jellyfin.apiclient.model.dto.ImageOptions
import org.jellyfin.apiclient.model.entities.ImageType
import org.jellyfin.apiclient.model.users.AuthenticationResult
import org.jellyfin.sdk.model.serializer.toUUID
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
				authenticationStore.putUser(server.id, user.id, storedUser.copy(
					name = user.name,
					requirePassword = user.requirePassword,
					imageTag = user.imageTag,
					lastUsed = Date().time
				))
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
			account?.accessToken != null -> when {
				// Update session
				setActiveSession(user, server) -> emit(AuthenticatedState)
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

		val result = try {
			callApi<AuthenticationResult> { callback ->
				val api = jellyfin.createApi(server.address, device = AuthenticationDevice(device, username))
				api.AuthenticateUserAsync(username, password, callback)
			}

			// Supress because com.android.volley.AuthFailureError is not exposed by the apiclient
		} catch (@Suppress("TooGenericExceptionCaught") err: Exception) {
			Timber.e(err, "Unable to sign in as $username")
			emit(RequireSignInState)
			return@flow
		}

		val userId = result.user.id.toUUID()
		val currentUser = authenticationStore.getUser(server.id, userId)
		val updatedUser = currentUser?.copy(
			name = result.user.name,
			lastUsed = Date().time,
			requirePassword = result.user.hasPassword,
			imageTag = result.user.primaryImageTag
		) ?: AuthenticationStoreUser(
			name = result.user.name,
			requirePassword = result.user.hasPassword,
			imageTag = result.user.primaryImageTag
		)

		authenticationStore.putUser(server.id, userId, updatedUser)
		accountManagerHelper.putAccount(AccountManagerAccount(userId, server.id, updatedUser.name, result.accessToken))

		val user = PrivateUser(
			id = userId,
			serverId = server.id,
			name = updatedUser.name,
			accessToken = result.accessToken,
			requirePassword = result.user.hasPassword,
			imageTag = result.user.primaryImageTag,
			lastUsed = Date().time,
		)
		setActiveSession(user, server)
		emit(AuthenticatedState)
	}

	override fun logout(user: User) {
		accountManagerHelper.getAccount(user.id)?.let(accountManagerHelper::removeAccount)
	}

	override fun removeUser(user: User) {
		authenticationStore.removeUser(user.serverId, user.id)
		logout(user)
	}

	override fun getUserImageUrl(server: Server, user: User): String? {
		val apiClient = jellyfin.createApi(serverAddress = server.address, device = device)
		return apiClient.GetUserImageUrl(user.id.toString(), ImageOptions().apply {
			tag = user.imageTag
			imageType = ImageType.Primary
			maxHeight = ImageUtils.MAX_PRIMARY_IMAGE_HEIGHT
		})
	}
}
