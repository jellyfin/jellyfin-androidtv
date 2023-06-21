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
import org.jellyfin.androidtv.preference.TelemetryPreferences
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.DISABLED
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.LAST_USER
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.SPECIFIC_USER
import org.jellyfin.androidtv.util.sdk.forUser
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.clientLogApi
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

enum class SessionRepositoryState {
	READY,
	RESTORING_SESSION,
	SWITCHING_SESSION,
}

interface SessionRepository {
	val currentSession: StateFlow<Session?>
	val state: StateFlow<SessionRepositoryState>

	suspend fun restoreSession()
	suspend fun switchCurrentSession(serverId: UUID, userId: UUID): Boolean
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
	private val serverRepository: ServerRepository,
	private val telemetryPreferences: TelemetryPreferences,
) : SessionRepository {
	private val currentSessionMutex = Mutex()
	private val _currentSession = MutableStateFlow<Session?>(null)
	override val currentSession = _currentSession.asStateFlow()
	private val _state = MutableStateFlow(SessionRepositoryState.READY)
	override val state = _state.asStateFlow()

	override suspend fun restoreSession(): Unit = currentSessionMutex.withLock {
		Timber.i("Restoring session")
		_state.value = SessionRepositoryState.RESTORING_SESSION

		if (authenticationPreferences[AuthenticationPreferences.alwaysAuthenticate]) return destroyCurrentSession()

		when (authenticationPreferences[AuthenticationPreferences.autoLoginUserBehavior]) {
			DISABLED -> destroyCurrentSession()
			LAST_USER -> setCurrentSession(createLastUserSession())
			SPECIFIC_USER -> {
				val serverId = authenticationPreferences[AuthenticationPreferences.autoLoginServerId].toUUIDOrNull()
				val userId = authenticationPreferences[AuthenticationPreferences.autoLoginUserId].toUUIDOrNull()
				if (serverId != null && userId != null) setCurrentSession(createUserSession(serverId, userId))
			}
		}

		_state.value = SessionRepositoryState.READY
	}

	override suspend fun switchCurrentSession(serverId: UUID, userId: UUID): Boolean {
		// No change in user - don't switch
		if (currentSession.value?.userId == userId) {
			Timber.d("Current session user is the same as the requested user")
			return false
		}

		_state.value = SessionRepositoryState.SWITCHING_SESSION
		Timber.i("Switching current session to user $userId")

		val session = createUserSession(serverId, userId)
		if (session == null) {
			Timber.w("Could not switch to non-existing session for user $userId")
			return false
		}

		val switched = setCurrentSession(session)
		_state.value = SessionRepositoryState.READY
		return switched
	}

	override fun destroyCurrentSession() {
		Timber.i("Destroying current session")

		userRepository.updateCurrentUser(null)
		_currentSession.value = null
		userApiClient.applySession(null)
		apiBinder.updateSession(null, userApiClient.deviceInfo)
		_state.value = SessionRepositoryState.READY
	}

	private suspend fun setCurrentSession(session: Session?): Boolean {
		if (session != null) {
			// No change in session - don't switch
			if (currentSession.value?.userId == session.userId) return true

			// Update last active user
			authenticationPreferences[AuthenticationPreferences.lastServerId] = session.serverId.toString()
			authenticationPreferences[AuthenticationPreferences.lastUserId] = session.userId.toString()

			// Check if server version is supported
			val server = serverRepository.getServer(session.serverId)
			if (server == null || !server.versionSupported) return false
		}

		// Update session after binding the apiclient settings
		val deviceInfo = session?.let { defaultDeviceInfo.forUser(it.userId) } ?: defaultDeviceInfo
		val success = apiBinder.updateSession(session, deviceInfo)
		Timber.i("Updating current session. userId=${session?.userId} apiBindingSuccess=${success}")

		if (success) {
			userApiClient.applySession(session, deviceInfo)

			if (session != null) {
				// Update crash reporting URL
				val crashReportUrl = userApiClient.clientLogApi.logFileUrl(includeCredentials = false)
				telemetryPreferences[TelemetryPreferences.crashReportUrl] = crashReportUrl
				telemetryPreferences[TelemetryPreferences.crashReportToken] = session.accessToken

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
		val lastServerId = authenticationPreferences[AuthenticationPreferences.lastServerId].toUUIDOrNull()

		return if (lastUserId != null && lastServerId != null) createUserSession(lastServerId, lastUserId)
		else null
	}

	private fun createUserSession(serverId: UUID, userId: UUID): Session? {
		val account = accountManagerStore.getAccount(serverId, userId)
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
