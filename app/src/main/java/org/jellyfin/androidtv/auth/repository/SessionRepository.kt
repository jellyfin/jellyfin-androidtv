package org.jellyfin.androidtv.auth.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jellyfin.androidtv.auth.apiclient.ApiBinder
import org.jellyfin.androidtv.auth.store.AccountManagerStore
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.DISABLED
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.LAST_USER
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.SPECIFIC_USER
import org.jellyfin.androidtv.util.sdk.forUser
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber
import java.util.UUID

data class Session(
	val userId: UUID,
	val serverId: UUID,
	val accessToken: String,
)

interface SessionRepository {
	val currentSession: StateFlow<Session?>

	suspend fun restoreSession()
	suspend fun switchCurrentSession(userId: UUID): Boolean
	fun destroyCurrentSession()
}

class SessionRepositoryImpl(
	private val authenticationPreferences: AuthenticationPreferences,
	private val accountManagerStore: AccountManagerStore,
	private val apiBinder: ApiBinder,
	private val authenticationStore: AuthenticationStore,
	private val userApiClient: ApiClient,
	private val preferencesRepository: PreferencesRepository,
	private val defaultDeviceInfo: DeviceInfo,
	private val userRepository: UserRepository,
) : SessionRepository {
	private val currentSessionMutex = Mutex()
	private val _currentSession = MutableStateFlow<Session?>(null)
	override val currentSession = _currentSession.asStateFlow()

	override suspend fun restoreSession(): Unit = currentSessionMutex.withLock {
		Timber.d("Restoring session")

		if (authenticationPreferences[AuthenticationPreferences.alwaysAuthenticate]) return destroyCurrentSession()

		val behavior = authenticationPreferences[AuthenticationPreferences.autoLoginUserBehavior]
		val userId = authenticationPreferences[AuthenticationPreferences.autoLoginUserId].toUUIDOrNull()

		when (behavior) {
			DISABLED -> destroyCurrentSession()
			LAST_USER -> setCurrentSession(createLastUserSession())
			SPECIFIC_USER -> setCurrentSession(createUserSession(userId))
		}
	}

	override suspend fun switchCurrentSession(userId: UUID): Boolean {
		// No change in user - don't switch
		if (currentSession.value?.userId == userId) return false

		Timber.d("Switching current session to user $userId")

		val session = createUserSession(userId)
		if (session == null) {
			Timber.d("Could not switch to non-existing session for user $userId")
			return false
		}

		return setCurrentSession(session)
	}

	override fun destroyCurrentSession() {
		Timber.d("Destroying current session")

		userRepository.updateCurrentUser(null)
		_currentSession.value = null
		userApiClient.applySession(null)
		apiBinder.updateSession(null, userApiClient.deviceInfo)
	}

	private suspend fun setCurrentSession(session: Session?): Boolean {
		// No change in session - don't switch
		if (session != null && currentSession.value?.userId == session.userId) return true
		// Update last active user
		if (session != null) authenticationPreferences[AuthenticationPreferences.lastUserId] = session.userId.toString()

		// Update session after binding the apiclient settings
		val deviceInfo = session?.let { defaultDeviceInfo.forUser(it.userId) } ?: defaultDeviceInfo
		val success = apiBinder.updateSession(session, deviceInfo)
		Timber.d("Updating current session. userId=${session?.userId} apiBindingSuccess=${success}")

		if (success) {
			userApiClient.applySession(session, deviceInfo)
			if (session != null) {
				try {
					val user by userApiClient.userApi.getCurrentUser()
					userRepository.updateCurrentUser(user)
				} catch (err: ApiClientException) {
					Timber.e(err, "Unable to authenticate: bad response when getting user info")
					destroyCurrentSession()
					return false
				}
			} else {
				userRepository.updateCurrentUser(null)
			}
			preferencesRepository.onSessionChanged()
			_currentSession.value = session
		} else destroyCurrentSession()

		return success
	}

	private fun createLastUserSession(): Session? {
		val lastUserId = authenticationPreferences[AuthenticationPreferences.lastUserId].toUUIDOrNull()
		return createUserSession(lastUserId)
	}

	private fun createUserSession(userId: UUID?): Session? {
		if (userId == null) return null

		val account = accountManagerStore.getAccount(userId)
		if (account?.accessToken == null) return null

		return Session(
			userId = account.id,
			serverId = account.server,
			accessToken = account.accessToken
		)
	}

	private fun ApiClient.applySession(session: Session?, newDeviceInfo: DeviceInfo = defaultDeviceInfo) {
		deviceInfo = newDeviceInfo

		if (session == null) {
			baseUrl = null
			accessToken = null
			userId = null
		} else {
			val server = authenticationStore.getServer(session.serverId)
				?: return applySession(null)
			baseUrl = server.address
			accessToken = session.accessToken
			userId = session.userId
		}
	}
}
