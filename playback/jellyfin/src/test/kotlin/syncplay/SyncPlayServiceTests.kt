@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.jellyfin.playback.jellyfin.syncplay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.PlayerCommand
import org.jellyfin.playback.core.PlayerState
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PositionInfo
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.QueueService
import org.jellyfin.playback.core.queue.initialPlayback
import org.jellyfin.playback.core.queue.supplier.QueueSupplier
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.operations.SyncPlayApi
import org.jellyfin.sdk.api.operations.TimeSyncApi
import org.jellyfin.sdk.api.operations.UserLibraryApi
import org.jellyfin.sdk.api.sockets.SocketApiState
import org.jellyfin.sdk.api.sockets.SocketApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.GroupRepeatMode
import org.jellyfin.sdk.model.api.GroupShuffleMode
import org.jellyfin.sdk.model.api.GroupStateType
import org.jellyfin.sdk.model.api.GroupUpdate
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.OutboundWebSocketMessage
import org.jellyfin.sdk.model.api.PlayQueueUpdate
import org.jellyfin.sdk.model.api.PlayQueueUpdateReason
import org.jellyfin.sdk.model.api.ReadyRequestDto
import org.jellyfin.sdk.model.api.SendCommand
import org.jellyfin.sdk.model.api.SendCommandType
import org.jellyfin.sdk.model.api.SyncPlayCommandMessage
import org.jellyfin.sdk.model.api.SyncPlayGroupJoinedUpdate
import org.jellyfin.sdk.model.api.SyncPlayGroupUpdateMessage
import org.jellyfin.sdk.model.api.SyncPlayNotInGroupUpdate
import org.jellyfin.sdk.model.api.SyncPlayPlayQueueUpdate
import org.jellyfin.sdk.model.api.SyncPlayQueueItem
import org.jellyfin.sdk.model.api.SyncPlayUserJoinedUpdate
import org.jellyfin.sdk.model.api.UtcTimeResponse
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SyncPlayServiceTests : FunSpec({
	test("a group queue prepares paused and reports the queue entry identifier when ready") {
		withService {
			service.createGroup("Movie night")
			loadQueue(position = 42.seconds)
			val entry = currentEntry.value.shouldNotBeNull()
			entry.initialPlayback.playWhenReady shouldBe false
			entry.initialPlayback.position shouldBe 42.seconds
			position = 42.seconds
			listener.captured.onMediaStreamReady(entry)
			testScope.runCurrent()

			val ready = slot<ReadyRequestDto>()
			coVerify(exactly = 1) { syncApi.syncPlayReady(capture(ready)) }
			ready.captured.playlistItemId shouldBe playlistItemId
			ready.captured.positionTicks shouldBe 420_000_000L
			ready.captured.isPlaying shouldBe false
			coVerify(exactly = 0) { syncApi.syncPlayUnpause() }
			verify(exactly = 0) { backend.play() }
		}
	}

	test("scheduled server commands change local playback without echoing group requests") {
		withService {
			service.createGroup("Movie night")
			loadQueue()
			sendCommand(SendCommandType.UNPAUSE, scheduledIn = 500)
			testScope.advanceTimeBy(499)
			testScope.runCurrent()
			verify(exactly = 0) { backend.play() }
			testScope.advanceTimeBy(1)
			testScope.runCurrent()
			verify(exactly = 1) { backend.play() }
			sendCommand(SendCommandType.PAUSE, position = 8.seconds)
			verify(exactly = 1) { backend.seekTo(8.seconds) }
			playState.value shouldBe PlayState.PAUSED
			coVerify(exactly = 0) { syncApi.syncPlayUnpause() }
			coVerify(exactly = 0) { syncApi.syncPlayPause() }
			coVerify(exactly = 0) { syncApi.syncPlaySeek(any()) }
		}
	}

	test("user playback controls send requests and ordinary playback is restored after leaving") {
		withService {
			service.createGroup("Movie night")
			service.handleCommand(PlayerCommand.Pause) shouldBe true
			testScope.runCurrent()
			coVerify(exactly = 1) { syncApi.syncPlayPause() }
			service.leaveGroup()
			service.handleCommand(PlayerCommand.Pause) shouldBe false
			coVerify(exactly = 1) { syncApi.syncPlayLeaveGroup() }
			verify { backend.setSpeed(1.5f) }
		}
	}

	test("leaving cancels a future unpause so a stale group cannot resume the local player") {
		withService {
			service.createGroup("Movie night")
			loadQueue()
			sendCommand(SendCommandType.UNPAUSE, scheduledIn = 1000)
			service.leaveGroup()
			testScope.advanceTimeBy(2000)
			testScope.runCurrent()
			verify(exactly = 0) { backend.play() }
			service.group.value shouldBe null
		}
	}

	test("the server's zero-group NotInGroup error clears local membership") {
		withService {
			service.createGroup("Movie night")
			sendUpdate(SyncPlayNotInGroupUpdate(UUID(0, 0), ""))
			service.group.value shouldBe null
			service.handleCommand(PlayerCommand.Play) shouldBe false
		}
	}

	test("a join timeout clears pending membership and ignores a late GroupJoined message") {
		withService {
			val join = testScope.async { service.joinGroup(groupInfo.groupId) }
			testScope.runCurrent()
			testScope.advanceTimeBy(10_001)
			testScope.runCurrent()
			join.await()
			service.busy.value shouldBe false
			service.error.value.shouldNotBeNull()
			sendUpdate(SyncPlayGroupJoinedUpdate(groupInfo.groupId, groupInfo))
			service.group.value shouldBe null
		}
	}

	test("refreshing group information does not invalidate a command issued after joining") {
		withService {
			service.createGroup("Movie night")
			loadQueue()
			coEvery { syncApi.syncPlayGetGroups() } returns response(listOf(groupInfo.copy(lastUpdatedAt = date(5000))))
			service.refreshGroups()
			sendCommand(SendCommandType.UNPAUSE, emittedAt = date(1000))
			verify(exactly = 1) { backend.play() }
		}
	}

	test("buffering pauses the group only after three seconds and recovery sends Ready") {
		withService {
			service.createGroup("Movie night")
			loadQueue()
			val entry = currentEntry.value.shouldNotBeNull()
			listener.captured.onMediaStreamReady(entry)
			testScope.runCurrent()
			sendCommand(SendCommandType.UNPAUSE)
			listener.captured.onBuffering(entry, true)
			testScope.runCurrent()
			testScope.advanceTimeBy(2999)
			testScope.runCurrent()
			coVerify(exactly = 0) { syncApi.syncPlayBuffering(any()) }
			testScope.advanceTimeBy(1)
			testScope.runCurrent()
			coVerify(exactly = 1) { syncApi.syncPlayBuffering(match { it.playlistItemId == playlistItemId && it.isPlaying }) }
			listener.captured.onMediaStreamReady(entry)
			testScope.runCurrent()
			coVerify(exactly = 2) { syncApi.syncPlayReady(match { it.playlistItemId == playlistItemId }) }
		}
	}

	test("brief buffering recovers without holding the group") {
		withService {
			service.createGroup("Movie night")
			loadQueue()
			val entry = currentEntry.value.shouldNotBeNull()
			listener.captured.onMediaStreamReady(entry)
			testScope.runCurrent()
			sendCommand(SendCommandType.UNPAUSE)
			listener.captured.onBuffering(entry, true)
			testScope.runCurrent()
			testScope.advanceTimeBy(1000)
			listener.captured.onMediaStreamReady(entry)
			testScope.advanceTimeBy(3000)
			testScope.runCurrent()
			coVerify(exactly = 0) { syncApi.syncPlayBuffering(any()) }
		}
	}

	test("a queue insertion preserves the active entry and updates its index and surrounding items") {
		withService {
			service.createGroup("Movie night")
			loadQueue()
			val entry = currentEntry.value.shouldNotBeNull()
			val addedItem = SyncPlayQueueItem(UUID.randomUUID(), UUID.randomUUID())
			val previousQueue = latestQueue.shouldNotBeNull()
			sendUpdate(
				SyncPlayPlayQueueUpdate(
					groupInfo.groupId,
					previousQueue.copy(
						reason = PlayQueueUpdateReason.QUEUE,
						lastUpdate = date(1),
						playlist = listOf(addedItem) + previousQueue.playlist,
						playingItemIndex = 1,
					),
				)
			)
			(currentEntry.value === entry) shouldBe true
			selectedIndex shouldBe 1
			suppliedQueue.shouldNotBeNull().size shouldBe 2
			suppliedQueue.shouldNotBeNull().getItem(0).shouldNotBeNull().baseItem.shouldNotBeNull().id shouldBe addedItem.itemId
			verify(exactly = 1) { backend.pause() }
		}
	}

	test("repeat-one restarts the same queue entry and waits for readiness") {
		withService {
			service.createGroup("Movie night")
			loadQueue(position = 100.seconds)
			val originalEntry = currentEntry.value.shouldNotBeNull()
			sendUpdate(SyncPlayPlayQueueUpdate(groupInfo.groupId, latestQueue.shouldNotBeNull().copy(
				reason = PlayQueueUpdateReason.NEXT_ITEM,
				lastUpdate = date(1),
				startPositionTicks = 0,
			)))
			val restartedEntry = currentEntry.value.shouldNotBeNull()
			(restartedEntry === originalEntry) shouldBe false
			restartedEntry.initialPlayback.position shouldBe Duration.ZERO
			restartedEntry.initialPlayback.playWhenReady shouldBe false
		}
	}

	test("a seek waiting behind another request cannot change a replacement playlist") {
		withService {
			service.createGroup("Movie night")
			loadQueue()
			val gate = CompletableDeferred<Unit>()
			coEvery { syncApi.syncPlayPause() } coAnswers { gate.await(); response(Unit) }
			service.handleCommand(PlayerCommand.Pause)
			testScope.runCurrent()
			service.handleCommand(PlayerCommand.Seek(80.seconds))
			testScope.runCurrent()
			loadQueue()
			gate.complete(Unit)
			testScope.runCurrent()
			coVerify(exactly = 0) { syncApi.syncPlaySeek(any()) }
		}
	}

	test("a queued item selection keeps its original identity across a queue insertion") {
		withService {
			service.createGroup("Movie night")
			loadQueue()
			val gate = CompletableDeferred<Unit>()
			coEvery { syncApi.syncPlayPause() } coAnswers { gate.await(); response(Unit) }
			service.handleCommand(PlayerCommand.Pause)
			testScope.runCurrent()
			service.handleCommand(PlayerCommand.Select(0))
			testScope.runCurrent()
			val oldQueue = latestQueue.shouldNotBeNull()
			sendUpdate(SyncPlayPlayQueueUpdate(groupInfo.groupId, oldQueue.copy(
				reason = PlayQueueUpdateReason.QUEUE,
				lastUpdate = date(1),
				playlist = listOf(SyncPlayQueueItem(UUID.randomUUID(), UUID.randomUUID())) + oldQueue.playlist,
				playingItemIndex = 1,
			)))
			gate.complete(Unit)
			testScope.runCurrent()
			coVerify(exactly = 1) { syncApi.syncPlaySetPlaylistItem(match { it.playlistItemId == playlistItemId }) }
		}
	}

	test("leaving cancels a request still in flight and removes command interception") {
		withService {
			service.createGroup("Movie night")
			val gate = CompletableDeferred<Unit>()
			var finished = false
			coEvery { syncApi.syncPlayPause() } coAnswers {
				try { gate.await(); response(Unit) } finally { finished = true }
			}
			service.handleCommand(PlayerCommand.Pause)
			testScope.runCurrent()
			finished shouldBe false
			service.leaveGroup()
			testScope.runCurrent()
			finished shouldBe true
			service.handleCommand(PlayerCommand.Play) shouldBe false
		}
	}

	test("service cancellation removes its listener and stale ready events cannot reach the server") {
		withService {
			service.createGroup("Movie night")
			loadQueue()
			val entry = currentEntry.value.shouldNotBeNull()
			scope.cancel()
			testScope.runCurrent()
			service.group.value shouldBe null
			verify(exactly = 1) { manager.removeBackendListener(listener.captured) }
			listener.captured.onMediaStreamReady(entry)
			testScope.runCurrent()
			coVerify(exactly = 0) { syncApi.syncPlayReady(any()) }
		}
	}

	test("a group list requested before logout cannot repopulate the next session") {
		withService {
			service.createGroup("Movie night")
			val gate = CompletableDeferred<Unit>()
			coEvery { syncApi.syncPlayGetGroups() } coAnswers { gate.await(); response(listOf(groupInfo)) }
			val refresh = testScope.async { service.refreshGroups() }
			testScope.runCurrent()
			service.endSession()
			gate.complete(Unit)
			refresh.await()
			service.groups.value shouldBe emptyList()
			service.group.value shouldBe null
		}
	}

	test("a slow participant refresh neither delays playback commands nor restores a departed group") {
		withService {
			service.createGroup("Movie night")
			loadQueue()
			val gate = CompletableDeferred<Unit>()
			coEvery { syncApi.syncPlayGetGroups() } coAnswers { gate.await(); response(listOf(groupInfo)) }
			sendUpdate(SyncPlayUserJoinedUpdate(groupInfo.groupId, "Second viewer"))
			sendCommand(SendCommandType.UNPAUSE)
			verify(exactly = 1) { backend.play() }
			service.endSession()
			gate.complete(Unit)
			testScope.runCurrent()
			service.group.value shouldBe null
		}
	}
})

private suspend fun withService(block: suspend ServiceFixture.() -> Unit) = runTest {
	val fixture = ServiceFixture(this)
	try {
		fixture.service.onInitialize()
		runCurrent()
		fixture.block()
	} finally {
		fixture.scope.cancel()
		runCurrent()
		unmockkObject(fixture.service)
		Dispatchers.resetMain()
	}
}

private class ServiceFixture(val testScope: TestScope) {
	private val epoch = 1_700_000_000_000L
	private val dispatcher = StandardTestDispatcher(testScope.testScheduler)
	val scope = CoroutineScope(SupervisorJob() + dispatcher)
	val syncApi = mockk<SyncPlayApi>()
	private val timeApi = mockk<TimeSyncApi>()
	private val libraryApi = mockk<UserLibraryApi>()
	private val api = mockk<ApiClient>()
	private val socket = mockk<SocketApi>(relaxed = true)
	private val messages = MutableSharedFlow<OutboundWebSocketMessage>(extraBufferCapacity = 16)
	private val socketState = MutableStateFlow<SocketApiState>(SocketApiState.Connected)
	val backend = mockk<PlayerBackend>(relaxed = true)
	val manager = mockk<PlaybackManager>(relaxed = true)
	private val state = mockk<PlayerState>(relaxed = true)
	private val queue = mockk<QueueService>(relaxed = true)
	val listener = slot<PlayerBackendEventListener>()
	val currentEntry = MutableStateFlow<QueueEntry?>(null)
	var suppliedQueue: QueueSupplier? = null
	var selectedIndex = -1
	var latestQueue: PlayQueueUpdate? = null
	val playState = MutableStateFlow(PlayState.PAUSED)
	var position = Duration.ZERO
	val playlistItemId = UUID.randomUUID()
	private val mediaId = UUID.randomUUID()
	val groupInfo = GroupInfoDto(UUID.randomUUID(), "Movie night", GroupStateType.IDLE, listOf("Viewer"), date(0))
	private val clock = SyncPlayClock({ epoch + testScope.testScheduler.currentTime }, { testScope.testScheduler.currentTime * 1_000_000 })
	val speed = MutableStateFlow(1.5f)
	val service = SyncPlayService(api, clock)

	init {
		Dispatchers.setMain(dispatcher)
		// Keep the real instance: copying a spy leaves backend listeners bound to the original.
		mockkObject(service)
		every { api.baseUrl } returns "https://jellyfin.example.test"
		every { api.accessToken } returns "test-token"
		every { api.webSocket } returns socket
		every { api.getOrCreateApi(SyncPlayApi::class, any()) } returns syncApi
		every { api.getOrCreateApi(TimeSyncApi::class, any()) } returns timeApi
		every { api.getOrCreateApi(UserLibraryApi::class, any()) } returns libraryApi
		every { socket.subscribeAll() } returns messages
		every { socket.state } returns socketState
		every { service.manager } returns manager
		every { service.state } returns state
		every { service.coroutineScope } returns scope
		every { manager.backend } returns backend
		every { manager.getService(QueueService::class) } returns queue
		every { manager.addBackendListener(capture(listener)) } returns Unit
		every { queue.entry } returns currentEntry
		every { state.playState } returns playState
		every { state.speed } returns speed
		every { state.positionInfo } answers { PositionInfo(position, position + 30.seconds, 3600.seconds) }
		every { backend.pause() } answers { playState.value = PlayState.PAUSED }
		every { backend.play() } answers { playState.value = PlayState.PLAYING }
		coEvery { timeApi.getUtcTime() } answers { response(UtcTimeResponse(date(), date())) }
		coEvery { syncApi.syncPlayCreateGroup(any()) } returns response(groupInfo)
		coEvery { syncApi.syncPlayGetGroups() } returns response(listOf(groupInfo))
		coEvery { syncApi.syncPlayJoinGroup(any()) } returns response(Unit)
		coEvery { syncApi.syncPlayLeaveGroup() } returns response(Unit)
		coEvery { syncApi.syncPlayReady(any()) } returns response(Unit)
		coEvery { syncApi.syncPlayPing(any()) } returns response(Unit)
		coEvery { syncApi.syncPlayPause() } returns response(Unit)
		coEvery { syncApi.syncPlayUnpause() } returns response(Unit)
		coEvery { syncApi.syncPlaySeek(any()) } returns response(Unit)
		coEvery { syncApi.syncPlaySetPlaylistItem(any()) } returns response(Unit)
		coEvery { syncApi.syncPlayStop() } returns response(Unit)
		coEvery { syncApi.syncPlayBuffering(any()) } returns response(Unit)
		coEvery { libraryApi.getItem(any(), any()) } answers {
			response(BaseItemDto(id = firstArg(), name = "Feature", mediaType = MediaType.VIDEO, type = BaseItemKind.MOVIE))
		}
		coEvery { queue.synchronize(any(), any()) } coAnswers {
			suppliedQueue = firstArg()
			selectedIndex = secondArg()
			firstArg<QueueSupplier>().getItem(secondArg()).also { currentEntry.value = it }
		}
	}

	fun date(offset: Long = testScope.testScheduler.currentTime): LocalDateTime =
		LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch + offset), ZoneOffset.UTC)

	suspend fun loadQueue(position: Duration = Duration.ZERO) {
		val update = PlayQueueUpdate(
			reason = PlayQueueUpdateReason.NEW_PLAYLIST,
			lastUpdate = date(),
			playlist = listOf(SyncPlayQueueItem(mediaId, playlistItemId)),
			playingItemIndex = 0,
			startPositionTicks = position.inWholeNanoseconds / 100,
			isPlaying = false,
			shuffleMode = GroupShuffleMode.SORTED,
			repeatMode = GroupRepeatMode.REPEAT_NONE,
		)
		latestQueue = update
		sendUpdate(SyncPlayPlayQueueUpdate(groupInfo.groupId, update))
	}

	suspend fun sendUpdate(update: GroupUpdate) {
		messages.emit(SyncPlayGroupUpdateMessage(update, UUID.randomUUID()))
		testScope.runCurrent()
	}

	suspend fun sendCommand(
		type: SendCommandType,
		scheduledIn: Long = 0,
		position: Duration = Duration.ZERO,
		emittedAt: LocalDateTime = date(),
	) {
		messages.emit(
			SyncPlayCommandMessage(
				data = SendCommand(
					groupId = groupInfo.groupId,
					playlistItemId = playlistItemId,
					`when` = date(testScope.testScheduler.currentTime + scheduledIn),
					positionTicks = position.inWholeNanoseconds / 100,
					command = type,
					emittedAt = emittedAt,
				),
				messageId = UUID.randomUUID(),
			)
		)
		testScope.runCurrent()
	}
}

private fun <T> response(content: T) = Response(content, 200, emptyMap())
