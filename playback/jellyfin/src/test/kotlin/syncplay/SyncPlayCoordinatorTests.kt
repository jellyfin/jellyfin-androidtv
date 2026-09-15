package org.jellyfin.playback.jellyfin.syncplay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.jellyfin.sdk.api.sockets.SocketApiState
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.SendCommandType
import org.jellyfin.sdk.model.api.SyncPlayGroupJoinedUpdate
import org.jellyfin.sdk.model.api.SyncPlayGroupLeftUpdate
import org.jellyfin.sdk.model.api.SyncPlayPlayQueueUpdate
import org.jellyfin.sdk.model.api.SyncPlayQueueItem
import org.jellyfin.sdk.model.api.UtcTimeResponse
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SyncPlayCoordinatorTests : FunSpec({
	test("M03 subscribes before Join and waits for authoritative membership") {
		runTest {
			val fixture = Fixture(this)
			val join = async { fixture.coordinator.join(groupId) }
			runCurrent()
			fixture.transport.joinedWithSubscriber shouldBe true
			fixture.coordinator.state.value.joining shouldBe true
			fixture.joined()
			join.await() shouldBe true
			fixture.coordinator.state.value.group?.groupId shouldBe groupId
		}
	}

	test("B01 loads paused and reports actual readiness before scheduled unpause") {
		runTest {
			val fixture = Fixture(this)
			fixture.join()
			fixture.emitQueue()
			runCurrent()
			fixture.player.prepared shouldBe listOf(occurrenceId)
			fixture.player.playing shouldBe false
			fixture.transport.requests.filterIsInstance<SyncPlayRequest.Ready>().single().positionTicks shouldBe 0L
			fixture.transport.events.emit(SyncPlayEvent.Command(command(whenMillis = 3000)))
			runCurrent()
			fixture.player.playing shouldBe false
			advanceTimeBy(2000)
			runCurrent()
			fixture.player.playing shouldBe true
			fixture.transport.requests.filterIsInstance<SyncPlayRequest.Unpause>().size shouldBe 0
		}
	}

	test("T05 command arriving before preparation completes is retained") {
		runTest {
			val fixture = Fixture(this)
			fixture.player.gate = CompletableDeferred()
			fixture.join()
			fixture.emitQueue()
			fixture.transport.events.emit(SyncPlayEvent.Command(command(whenMillis = 2000)))
			runCurrent()
			advanceTimeBy(2000)
			runCurrent()
			fixture.player.playing shouldBe false
			fixture.player.gate?.complete(Unit)
			runCurrent()
			fixture.player.playing shouldBe true
		}
	}

	test("L05 leaving during preparation cancels readiness and future playback") {
		runTest {
			val fixture = Fixture(this)
			fixture.player.gate = CompletableDeferred()
			fixture.join()
			fixture.emitQueue()
			runCurrent()
			fixture.coordinator.leave()
			fixture.player.gate?.complete(Unit)
			advanceTimeBy(5000)
			runCurrent()
			fixture.coordinator.state.value.group shouldBe null
			fixture.transport.requests.filterIsInstance<SyncPlayRequest.Ready>() shouldBe emptyList()
			fixture.player.playing shouldBe false
		}
	}

	test("B05 failed preparation halts following and releases group wait") {
		runTest {
			val fixture = Fixture(this)
			fixture.player.fail = true
			fixture.join()
			fixture.emitQueue()
			runCurrent()
			fixture.coordinator.state.value.following shouldBe false
			fixture.coordinator.state.value.error shouldBe SyncPlayFailure.PLAYBACK
			fixture.transport.requests.last() shouldBe SyncPlayRequest.IgnoreWait(true)
		}
	}

	test("F08 halt remains a member and resume prepares fresh group playback") {
		runTest {
			val fixture = Fixture(this)
			fixture.join()
			fixture.emitQueue()
			runCurrent()
			fixture.coordinator.halt()
			fixture.coordinator.state.value.group?.groupId shouldBe groupId
			fixture.coordinator.state.value.following shouldBe false
			fixture.transport.requests.last() shouldBe SyncPlayRequest.IgnoreWait(true)
			fixture.coordinator.resume()
			runCurrent()
			fixture.player.prepared.size shouldBe 2
			fixture.coordinator.state.value.following shouldBe true
		}
	}

	test("L03 socket loss invalidates scheduled playback and requires rejoin") {
		runTest {
			val fixture = Fixture(this)
			fixture.join()
			fixture.emitQueue()
			fixture.transport.events.emit(SyncPlayEvent.Command(command(whenMillis = 3000)))
			runCurrent()
			fixture.transport.connectionState.value = SocketApiState.Disconnected()
			runCurrent()
			advanceTimeBy(3000)
			runCurrent()
			fixture.player.playing shouldBe false
			fixture.coordinator.state.value.error shouldBe SyncPlayFailure.DISCONNECTED
			fixture.coordinator.state.value.group shouldBe null
		}
	}

	test("M05 join failure ends pending state without a retry") {
		runTest {
			val fixture = Fixture(this)
			fixture.transport.failJoin = true
			fixture.coordinator.join(groupId) shouldBe false
			fixture.coordinator.state.value.joining shouldBe false
			fixture.coordinator.state.value.error shouldBe SyncPlayFailure.REQUEST
			fixture.transport.joinCount shouldBe 1
		}
	}

	test("Q03 an edit to the queue does not reload the active occurrence") {
		runTest {
			val fixture = Fixture(this)
			fixture.join()
			fixture.emitQueue()
			runCurrent()
			fixture.transport.events.emit(SyncPlayEvent.GroupUpdate(SyncPlayPlayQueueUpdate(groupId, queue(1400))))
			runCurrent()
			fixture.player.prepared.size shouldBe 1
		}
	}

	test("A01 user controls send group requests without directly starting local playback") {
		runTest {
			val fixture = Fixture(this)
			fixture.join()
			fixture.emitQueue()
			runCurrent()
			fixture.coordinator.request(SyncPlayRequest.Unpause)
			fixture.transport.requests.last() shouldBe SyncPlayRequest.Unpause
			fixture.player.playing shouldBe false
		}
	}
})

@OptIn(ExperimentalCoroutinesApi::class)
private class Fixture(private val scope: TestScope) {
	val transport = FakeSyncPlayTransport()
	val player = FakeSyncPlayPlayer()
	val coordinator = SyncPlayCoordinator(
		scope.backgroundScope, transport,
		{ scope.testScheduler.currentTime + 1000 }, { scope.testScheduler.currentTime },
	).also { it.attach(player) }

	suspend fun joined() {
		transport.events.emit(SyncPlayEvent.GroupUpdate(SyncPlayGroupJoinedUpdate(groupId, group())))
		scope.runCurrent()
	}

	suspend fun join() {
		val job = scope.async { coordinator.join(groupId) }
		scope.runCurrent()
		joined()
		job.await() shouldBe true
	}

	suspend fun emitQueue() {
		transport.events.emit(SyncPlayEvent.GroupUpdate(SyncPlayPlayQueueUpdate(groupId, queue())))
	}
}

private class FakeSyncPlayTransport : SyncPlayTransport {
	override val events = MutableSharedFlow<SyncPlayEvent>(extraBufferCapacity = 32)
	override val connectionState = MutableStateFlow<SocketApiState>(SocketApiState.Connected)
	val requests = mutableListOf<SyncPlayRequest>()
	var joinedWithSubscriber = false
	var failJoin = false
	var joinCount = 0
	override suspend fun groups() = listOf(group())
	override suspend fun createGroup(name: String): GroupInfoDto = group()
	override suspend fun joinGroup(id: UUID) {
		joinCount++
		joinedWithSubscriber = events.subscriptionCount.value > 0
		check(!failJoin)
	}
	override suspend fun leaveGroup() {
		events.emit(SyncPlayEvent.GroupUpdate(SyncPlayGroupLeftUpdate(groupId, groupId.toString())))
	}
	override suspend fun time() = UtcTimeResponse(date(1000), date(1000))
	override suspend fun send(request: SyncPlayRequest) { requests += request }
}

private class FakeSyncPlayPlayer : SyncPlayPlayer {
	override val events = MutableSharedFlow<SyncPlayPlayerEvent>(extraBufferCapacity = 8)
	var playing = false
	var position = 0L
	var gate: CompletableDeferred<Unit>? = null
	var fail = false
	val prepared = mutableListOf<UUID>()
	override val snapshot get() = SyncPlayPlayerSnapshot(position, playing)
	override suspend fun prepare(item: SyncPlayQueueItem, positionTicks: Long) {
		check(!fail)
		gate?.await()
		prepared += item.playlistItemId
		position = positionTicks
		playing = false
	}
	override suspend fun seek(positionTicks: Long) { position = positionTicks; playing = false }
	override fun pause() { playing = false }
	override fun unpause() { playing = true }
	override fun stop() { playing = false }
}
