package org.jellyfin.androidtv.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.preference.AuthenticationPreferences
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.*
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber
import java.util.*

data class Session(
	val userId: UUID,
	val serverId: UUID,
	val accessToken: String
)

interface SessionRepository {
	val currentSession: LiveData<Session?>
	val currentSystemSession: LiveData<Session?>

	fun restoreDefaultSession()
	fun restoreDefaultSystemSession()

	fun switchCurrentSession(userId: UUID)
	fun destroyCurrentSession()
}

class SessionRepositoryImpl(
	private val authenticationPreferences: AuthenticationPreferences,
	private val accountManagerHelper: AccountManagerHelper,
	private val apiBinder: ApiBinder,
) : SessionRepository {
	private val _currentSession = MutableLiveData<Session?>()
	override val currentSession: LiveData<Session?> get() = _currentSession

	private val _currentSystemSession = MutableLiveData<Session?>()
	override val currentSystemSession: LiveData<Session?> get() = _currentSystemSession

	override fun restoreDefaultSession() {
		Timber.d("Restoring default session")

		val behavior = authenticationPreferences[AuthenticationPreferences.autoLoginUserBehavior]
		val userId = authenticationPreferences[AuthenticationPreferences.autoLoginUserId].toUUIDOrNull()

		when (behavior) {
			DISABLED -> setCurrentSessionSync(null, false)
			LAST_USER -> setCurrentSessionSync(createLastUserSession(), false)
			SPECIFIC_USER -> setCurrentSessionSync(createUserSession(userId), false)
		}
	}

	override fun restoreDefaultSystemSession() {
		Timber.d("Restoring default system session")

		val behavior = authenticationPreferences[AuthenticationPreferences.systemUserBehavior]
		val userId = authenticationPreferences[AuthenticationPreferences.systemUserId].toUUIDOrNull()

		when (behavior) {
			DISABLED -> _currentSystemSession.postValue(null)
			LAST_USER -> _currentSystemSession.postValue(createLastUserSession())
			SPECIFIC_USER -> _currentSystemSession.postValue(createUserSession(userId))
		}
	}

	override fun switchCurrentSession(userId: UUID) {
		Timber.d("Switching current session to user ${userId}")

		val session = createUserSession(userId)
		if (session == null) {
			Timber.d("Could not switch to non-existing session for user ${userId}")
			return
		}

		setCurrentSession(session, true)
	}

	override fun destroyCurrentSession() {
		Timber.d("Destroying current session")

		setCurrentSession(null, false)
	}

	private fun setCurrentSessionSync(session: Session?, includeSystemUser: Boolean) = runBlocking {
		withContext(Dispatchers.IO) {
			setCurrentSession(session, includeSystemUser)
		}
	}

	private fun setCurrentSession(session: Session?, includeSystemUser: Boolean) {
		if (session != null) authenticationPreferences[AuthenticationPreferences.lastUserId] = session.userId.toString()

		val systemUserBehavior = authenticationPreferences[AuthenticationPreferences.systemUserBehavior]
		if (includeSystemUser && systemUserBehavior == LAST_USER) _currentSystemSession.postValue(session)

		// Update session after binding the apiclient settings
		apiBinder.updateSession(session) { success ->
			Timber.d("Updating current session. userId=${session?.userId} apiBindingSuccess=${success}")

			if (success) _currentSession.postValue(session)
			else _currentSession.postValue(null)
		}
	}

	private fun createLastUserSession(): Session? {
		val lastUserId = authenticationPreferences[AuthenticationPreferences.lastUserId].toUUIDOrNull()
		return createUserSession(lastUserId)
	}

	private fun createUserSession(userId: UUID?): Session? {
		if (userId == null) return null

		val account = accountManagerHelper.getAccount(userId)
		if (account == null || account.accessToken == null) return null

		return Session(
			userId = account.id,
			serverId = account.server,
			accessToken = account.accessToken
		)
	}
}
