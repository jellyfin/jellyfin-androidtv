package org.jellyfin.androidtv.ui.syncplay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jellyfin.androidtv.auth.repository.Session
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayClient
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayStatus
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayRequest
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.GroupStateType
import java.time.LocalDateTime
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SyncPlayViewModelTests : FunSpec({
	afterTest { Dispatchers.resetMain() }

	test("show loads the available groups") {
		runTest {
			Dispatchers.setMain(StandardTestDispatcher(testScheduler))
			val group = group("Movie night")
			val client = FakeSyncPlayClient(availableGroups = listOf(group))
			val sessions = MutableStateFlow(session())
			val viewModel = SyncPlayViewModel(client, repository(sessions))

			viewModel.show()
			advanceUntilIdle()

			viewModel.uiState.value.visible shouldBe true
			viewModel.uiState.value.groups.shouldContainExactly(group)
			viewModel.uiState.value.loading shouldBe false
		}
	}

	test("group loading failure is represented without exposing exception text") {
		runTest {
			Dispatchers.setMain(StandardTestDispatcher(testScheduler))
			val client = FakeSyncPlayClient(groupsError = IllegalStateException("secret server response"))
			val viewModel = SyncPlayViewModel(client, repository(MutableStateFlow(session())))

			viewModel.show()
			advanceUntilIdle()

			viewModel.uiState.value.error shouldBe SyncPlayUiError.GROUPS
			viewModel.uiState.value.loading shouldBe false
		}
	}

	test("join prevents a second membership mutation while active") {
		runTest {
			Dispatchers.setMain(StandardTestDispatcher(testScheduler))
			val selected = group("Friends")
			val client = FakeSyncPlayClient(availableGroups = listOf(selected))
			val viewModel = SyncPlayViewModel(client, repository(MutableStateFlow(session())))

			viewModel.join(selected.groupId)
			viewModel.join(UUID.randomUUID())
			advanceUntilIdle()

			client.joined.shouldContainExactly(selected.groupId)
		}
	}

	test("create rejects blank names and trims valid names") {
		runTest {
			Dispatchers.setMain(StandardTestDispatcher(testScheduler))
			val client = FakeSyncPlayClient()
			val viewModel = SyncPlayViewModel(client, repository(MutableStateFlow(session())))

			viewModel.create("   ")
			viewModel.create("  Family room  ")
			advanceUntilIdle()

			client.created.shouldContainExactly("Family room")
		}
	}

	test("session replacement closes membership and clears dialog data") {
		runTest {
			Dispatchers.setMain(StandardTestDispatcher(testScheduler))
			val sessions = MutableStateFlow(session())
			val client = FakeSyncPlayClient(availableGroups = listOf(group("Old server")))
			val viewModel = SyncPlayViewModel(client, repository(sessions))
			viewModel.show()
			advanceUntilIdle()

			sessions.value = session()
			advanceUntilIdle()

			client.closeCount shouldBe 1
			viewModel.uiState.value shouldBe SyncPlayUiState()
		}
	}

	test("leave halt and resume delegate once") {
		runTest {
			Dispatchers.setMain(StandardTestDispatcher(testScheduler))
			val client = FakeSyncPlayClient()
			val viewModel = SyncPlayViewModel(client, repository(MutableStateFlow(session())))

			viewModel.halt()
			advanceUntilIdle()
			viewModel.resume()
			advanceUntilIdle()
			viewModel.leave()
			advanceUntilIdle()

			client.haltCount shouldBe 1
			client.resumeCount shouldBe 1
			client.leaveCount shouldBe 1
		}
	}
})

private fun repository(sessions: StateFlow<Session?>) = mockk<SessionRepository>(relaxed = true) {
	every { currentSession } returns sessions
}

private fun session() = Session(UUID.randomUUID(), UUID.randomUUID(), "token")

private fun group(name: String) = GroupInfoDto(
	groupId = UUID.randomUUID(),
	groupName = name,
	state = GroupStateType.IDLE,
	participants = listOf("Alice"),
	lastUpdatedAt = LocalDateTime.MIN,
)

private class FakeSyncPlayClient(
	private val availableGroups: List<GroupInfoDto> = emptyList(),
	private val groupsError: Exception? = null,
) : SyncPlayClient {
	override val state = MutableStateFlow(SyncPlayStatus())
	val joined = mutableListOf<UUID>()
	val created = mutableListOf<String>()
	var leaveCount = 0
	var haltCount = 0
	var resumeCount = 0
	var closeCount = 0

	override suspend fun groups(): List<GroupInfoDto> = groupsError?.let { throw it } ?: availableGroups
	override suspend fun create(name: String): Boolean { created += name; return true }
	override suspend fun join(id: UUID): Boolean { joined += id; return true }
	override suspend fun leave() { leaveCount++ }
	override suspend fun halt() { haltCount++ }
	override suspend fun resume() { resumeCount++ }
	override suspend fun request(request: SyncPlayRequest) = true
	override fun close() { closeCount++; state.value = SyncPlayStatus() }
}
