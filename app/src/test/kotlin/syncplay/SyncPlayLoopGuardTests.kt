package syncplay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.syncplay.SyncPlayLoopGuard
import org.jellyfin.sdk.model.serializer.toUUID

class SyncPlayLoopGuardTests : FunSpec({
	test("queue state duplicates are ignored within guard window") {
		var now = 10_000L
		val guard = SyncPlayLoopGuard(nowMs = { now })
		val state = SyncPlayLoopGuard.QueueStateKey(
			playlistItemId = "689eb948-a547-4bc0-8abd-8cc2ce2cc647".toUUID(),
			index = 0,
			startPositionTicks = 0,
			isPlaying = false,
		)

		guard.shouldProcessQueueState(state) shouldBe true
		now += 300
		guard.shouldProcessQueueState(state) shouldBe false
		now += 1_600
		guard.shouldProcessQueueState(state) shouldBe true
	}

	test("ready duplicates are ignored within guard window") {
		var now = 20_000L
		val guard = SyncPlayLoopGuard(nowMs = { now })
		val ready = SyncPlayLoopGuard.ReadyStateKey(
			playlistItemId = "689eb948-a547-4bc0-8abd-8cc2ce2cc647".toUUID(),
			positionTicks = 0,
			isPlaying = false,
		)

		guard.shouldSendReady(ready) shouldBe true
		now += 200
		guard.shouldSendReady(ready) shouldBe false
		now += 1_100
		guard.shouldSendReady(ready) shouldBe true
	}

	test("changed state always passes immediately") {
		val guard = SyncPlayLoopGuard(nowMs = { 30_000L })
		val baseItemId = "689eb948-a547-4bc0-8abd-8cc2ce2cc647".toUUID()
		val otherItemId = "c9ab0f51-80d5-4866-8830-89e7553951d3".toUUID()

		guard.shouldProcessQueueState(
			SyncPlayLoopGuard.QueueStateKey(baseItemId, 0, 0, false)
		) shouldBe true
		guard.shouldProcessQueueState(
			SyncPlayLoopGuard.QueueStateKey(otherItemId, 0, 0, false)
		) shouldBe true

		guard.shouldSendReady(SyncPlayLoopGuard.ReadyStateKey(baseItemId, 0, false)) shouldBe true
		guard.shouldSendReady(SyncPlayLoopGuard.ReadyStateKey(baseItemId, 10_000, false)) shouldBe true
	}
})
