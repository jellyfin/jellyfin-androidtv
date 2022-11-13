package org.jellyfin.androidtv.auth.repository

import arrow.core.Either
import arrow.core.continuations.either
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
import org.jellyfin.sdk.api.client.exception.TimeoutException
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

sealed class SessionError {
	object ServerOffline : SessionError()
	object UnsupportedServerVersion : SessionError()
	object AuthenticationError : SessionError()
	object Uninitialized : SessionError()
	/// Requested destruction by client code
	object Destroyed : SessionError()
}

interface SessionRepository {
	val currentSession: StateFlow<Either<SessionError, Session>>

	suspend fun restoreSession(): Either<SessionError, Unit>
	suspend fun switchCurrentSession(userId: UUID): Either<SessionError, Unit>
	suspend fun destroyCurrentSession()
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
	private val _currentSession = MutableStateFlow<Either<SessionError, Session>>(Either.Left(
		SessionError.Uninitialized
	))
	override val currentSession = _currentSession.asStateFlow()


	override suspend fun restoreSession(): Either<SessionError, Unit> = currentSessionMutex.withLock {
		either {
			Timber.d("Restoring session")

			if (authenticationPreferences[AuthenticationPreferences.alwaysAuthenticate])
				Either.Right(destroyCurrentSessionInternal())

			when (authenticationPreferences[AuthenticationPreferences.autoLoginUserBehavior]) {
				DISABLED -> destroyCurrentSessionInternal()
				// TODO: Handle UUID null by destroying session
				LAST_USER -> {
					val lastUserId =
						authenticationPreferences[AuthenticationPreferences.lastUserId].toUUIDOrNull()
					lastUserId?.let {
						setCurrentSession(createUserSession(it).bind()).bind()
					}
				}
				// TODO: Handle UUID null by destroying session
				SPECIFIC_USER -> {
					val userId =
						authenticationPreferences[AuthenticationPreferences.autoLoginUserId].toUUIDOrNull()
					userId?.let {
						setCurrentSession(createUserSession(it).bind()).bind()
					}
				}
			}
		}
	}

	override suspend fun switchCurrentSession(userId: UUID): Either<SessionError, Unit> = currentSessionMutex.withLock {
		either {
			// No change in user - don't switch
			val currentSessionValue = currentSession.value
			if (currentSessionValue is Either.Right<Session?>
				&& currentSessionValue.value?.userId == userId
			) {
				Either.Right(Unit)
			}

			Timber.d("Switching current session to user $userId")

			val session = createUserSession(userId).tapLeft {
				Timber.d("Could not switch to non-existing session for user $userId, error: $it")
			}.bind()

			val switched = setCurrentSession(session)
			switched.bind()
		}
	}


	override suspend fun destroyCurrentSession() = currentSessionMutex.withLock {
		destroyCurrentSessionInternal()
		_currentSession.value = Either.Left(SessionError.Destroyed)
	}

	private fun destroyCurrentSessionInternal() {
		userRepository.updateCurrentUser(null)
		userApiClient.applySession(null)
		apiBinder.updateSession(null, userApiClient.deviceInfo)
		Timber.d("Destroyed current session")
	}

	private suspend fun setCurrentSession(session: Session): Either<SessionError, Unit> {
		// No change in session - don't switch
		val currentSessionValue = currentSession.value
		if (currentSessionValue is Either.Right<Session?>
			&& currentSessionValue.value?.userId == session.userId) {
			return Either.Right(Unit)
		}

		// Update last active user
		authenticationPreferences[AuthenticationPreferences.lastUserId] = session.userId.toString()

		// Check if server is reachable and version is supported
		when (val serverResult = serverRepository.getServer(session.serverId)) {
			is Either.Left -> {
				when (serverResult.value) {
					ServerRetrievalError.UnknownServer -> TODO()
					ServerRetrievalError.Unreachable -> {
						_currentSession.value = Either.Left(SessionError.ServerOffline)
						return Either.Left(SessionError.ServerOffline)
					}
				}
			}
			is Either.Right -> {
				if (!serverResult.value.versionSupported) {
					_currentSession.value = Either.Left(SessionError.UnsupportedServerVersion)
					return Either.Left(SessionError.UnsupportedServerVersion)
				}
			}
		}

		// Update session after binding the apiclient settings
		val deviceInfo = defaultDeviceInfo.forUser(session.userId)

		val success = apiBinder.updateSession(session, deviceInfo)
		Timber.d("Updated current session. userId=${session.userId} apiBindingSuccess=${success}")

		if (!success) {
			Timber.e("Failed to update apiBinder session")
			destroyCurrentSessionInternal()
			// TODO: Proper error return value?
			_currentSession.value = Either.Left(SessionError.AuthenticationError)
			return Either.Left(SessionError.AuthenticationError)
		}

		userApiClient.applySession(session, deviceInfo)

		// Update crash reporting URL
		val crashReportUrl = userApiClient.clientLogApi.logFileUrl(includeCredentials = false)
		telemetryPreferences[TelemetryPreferences.crashReportUrl] = crashReportUrl
		telemetryPreferences[TelemetryPreferences.crashReportToken] = session.accessToken

		try {
			val user by userApiClient.userApi.getCurrentUser()
			userRepository.updateCurrentUser(user)
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to authenticate: bad response when getting user info")
			destroyCurrentSessionInternal()

			// TODO: Differentiate on Error here
			val error = when (err) {
				is TimeoutException -> SessionError.ServerOffline
				else -> SessionError.AuthenticationError
			}

			_currentSession.value = Either.Left(error)
			return Either.Left(error)
		}
		preferencesRepository.onSessionChanged()
		_currentSession.value = Either.Right(session)
		return Either.Right(Unit)
	}

	private fun createUserSession(userId: UUID): Either<SessionError, Session> {
		val account = accountManagerStore.getAccount(userId)
		if (account?.accessToken == null) return Either.Left(SessionError.AuthenticationError)

		return Either.Right(
			Session(
				userId = account.id,
				serverId = account.server,
				accessToken = account.accessToken
			)
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
