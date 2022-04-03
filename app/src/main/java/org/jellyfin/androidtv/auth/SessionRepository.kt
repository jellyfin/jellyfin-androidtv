package org.jellyfin.androidtv.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.preference.AuthenticationPreferences
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.DISABLED
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.LAST_USER
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.SPECIFIC_USER
import org.jellyfin.androidtv.util.sdk.forUser
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class Session(
	val userId: UUID,
	val serverId: UUID,
	val accessToken: String,
)

interface SessionRepository {
	val currentSession: StateFlow<Session?>
	val currentSystemSession: StateFlow<Session?>

	fun restoreDefaultSession()
	fun restoreDefaultSystemSession()

	suspend fun switchCurrentSession(userId: UUID): Boolean
	fun destroyCurrentSession()
}

class SessionRepositoryImpl(
	private val authenticationPreferences: AuthenticationPreferences,
	private val accountManagerHelper: AccountManagerHelper,
	private val apiBinder: ApiBinder,
	private val authenticationStore: AuthenticationStore,
	private val userApiClient: ApiClient,
	private val systemApiClient: ApiClient,
	private val preferencesRepository: PreferencesRepository,
	private val defaultDeviceInfo: DeviceInfo,
	private val userRepository: UserRepository,
) : SessionRepository {
	private val _currentSession = MutableStateFlow<Session?>(null)
	override val currentSession: StateFlow<Session?> get() = _currentSession

	private val _currentSystemSession = MutableStateFlow<Session?>(null)
	override val currentSystemSession: StateFlow<Session?> get() = _currentSystemSession

	override fun restoreDefaultSession() {
		Timber.d("Restoring default session")

		if (authenticationPreferences[AuthenticationPreferences.alwaysAuthenticate]) return destroyCurrentSession()

		val behavior = authenticationPreferences[AuthenticationPreferences.autoLoginUserBehavior]
		val userId = authenticationPreferences[AuthenticationPreferences.autoLoginUserId].toUUIDOrNull()

		when (behavior) {
			DISABLED -> destroyCurrentSession()
			LAST_USER -> setCurrentSession(createLastUserSession(), false)
			SPECIFIC_USER -> setCurrentSession(createUserSession(userId), false)
		}
	}

	override fun restoreDefaultSystemSession() {
		Timber.d("Restoring default system session")

		if (authenticationPreferences[AuthenticationPreferences.alwaysAuthenticate]) return setCurrentSession(null, false)

		val behavior = authenticationPreferences[AuthenticationPreferences.systemUserBehavior]
		val userId = authenticationPreferences[AuthenticationPreferences.systemUserId].toUUIDOrNull()

		when (behavior) {
			DISABLED -> setCurrentSystemSession(null)
			LAST_USER -> setCurrentSystemSession(createLastUserSession())
			SPECIFIC_USER -> setCurrentSystemSession(createUserSession(userId))
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

		return suspendCoroutine { continuation ->
			setCurrentSession(session, true) { success ->
				continuation.resume(success)
			}
		}
	}

	override fun destroyCurrentSession() {
		Timber.d("Destroying current session")

		userRepository.updateCurrentUser(null)
		_currentSession.value = null
		userApiClient.applySession(null)
		apiBinder.updateSession(null, userApiClient.deviceInfo)
	}

	private fun setCurrentSession(session: Session?, includeSystemUser: Boolean, callback: ((Boolean) -> Unit)? = null) {
		// No change in session - don't switch
		if (session != null && currentSession.value?.userId == session.userId) return

		if (session != null) authenticationPreferences[AuthenticationPreferences.lastUserId] = session.userId.toString()

		val systemUserBehavior = authenticationPreferences[AuthenticationPreferences.systemUserBehavior]
		if (includeSystemUser && systemUserBehavior == LAST_USER) setCurrentSystemSession(session)

		// Update session after binding the apiclient settings
		val deviceInfo = session?.let { defaultDeviceInfo.forUser(it.userId) } ?: defaultDeviceInfo
		val success = apiBinder.updateSession(session, deviceInfo)
		Timber.d("Updating current session. userId=${session?.userId} apiBindingSuccess=${success}")

		if (success) runBlocking {
			userApiClient.applySession(session, deviceInfo)
			if (session != null) {
				val user by userApiClient.userApi.getCurrentUser()
				userRepository.updateCurrentUser(user)
			} else {
				userRepository.updateCurrentUser(null)
			}
			preferencesRepository.onSessionChanged()
			_currentSession.value = session
		} else {
			userRepository.updateCurrentUser(null)
			userApiClient.applySession(null, deviceInfo)
			_currentSession.value = null
		}

		callback?.invoke(success)
	}

	private fun setCurrentSystemSession(session: Session?) {
		// No change in session - don't switch
		if (session != null && currentSession.value?.userId == session.userId) return

		_currentSystemSession.value = session

		systemApiClient.applySession(session)
	}

	private fun createLastUserSession(): Session? {
		val lastUserId = authenticationPreferences[AuthenticationPreferences.lastUserId].toUUIDOrNull()
		return createUserSession(lastUserId)
	}

	private fun createUserSession(userId: UUID?): Session? {
		if (userId == null) return null

		val account = accountManagerHelper.getAccount(userId)
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
