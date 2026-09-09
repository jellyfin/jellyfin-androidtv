package org.jellyfin.androidtv.auth.repository

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.androidtv.auth.model.PrivateUser
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import java.util.UUID

class AuthenticationLogoutTests : FunSpec({
	test("signing out the active user finishes playback before forgetting credentials") {
		val user = storedUser()
		val session = Session(user.id, user.serverId, requireNotNull(user.accessToken))
		val sessions = mockk<SessionRepository>()
		val store = mockk<AuthenticationStore>()
		val actions = mutableListOf<String>()
		every { sessions.currentSession } returns MutableStateFlow(session)
		coEvery { sessions.destroyCurrentSession(session) } answers {
			actions.add("end session")
			Unit
		}
		every { store.getUser(user.serverId, user.id) } returns AuthenticationStoreUser(user.name, accessToken = user.accessToken)
		every { store.putUser(user.serverId, user.id, any()) } answers {
			thirdArg<AuthenticationStoreUser>().accessToken shouldBe null
			actions.add("forget credentials")
			true
		}

		logoutRepository(sessions, store).logout(user) shouldBe true

		actions shouldBe listOf("end session", "forget credentials")
		coVerify(exactly = 1) { sessions.destroyCurrentSession(expectedSession = session) }
	}

	test("signing out a stored user on another server preserves the active session") {
		val user = storedUser()
		val sessions = mockk<SessionRepository>()
		val store = mockk<AuthenticationStore>()
		every { sessions.currentSession } returns MutableStateFlow(Session(user.id, UUID.randomUUID(), "other-token"))
		every { store.getUser(user.serverId, user.id) } returns AuthenticationStoreUser(user.name, accessToken = user.accessToken)
		every { store.putUser(user.serverId, user.id, any()) } returns true

		logoutRepository(sessions, store).logout(user) shouldBe true

		coVerify(exactly = 0) { sessions.destroyCurrentSession(any()) }
		verify { store.putUser(user.serverId, user.id, match { it.accessToken == null }) }
	}
})

private fun storedUser() = PrivateUser(UUID.randomUUID(), UUID.randomUUID(), "Viewer", "test-token", null, 0)

private fun logoutRepository(sessions: SessionRepository, store: AuthenticationStore) = AuthenticationRepositoryImpl(
	jellyfin = mockk(),
	sessionRepository = sessions,
	authenticationStore = store,
	userApiClient = mockk(),
	authenticationPreferences = mockk(),
	defaultDeviceInfo = mockk(),
)
