package org.jellyfin.playback.jellyfin.syncplay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.jellyfin.sdk.model.api.SendCommandType
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SyncPlayCommandSchedulerTests : FunSpec({
	test("T06 does not unpause before its deadline and executes once") {
		runTest {
			val actions = mutableListOf<SyncPlayAction>()
			val scheduler = SyncPlayCommandScheduler(backgroundScope, { Instant.ofEpochMilli(testScheduler.currentTime) }, actions::add)
			scheduler.schedule(command())
			scheduler.schedule(command())
			runCurrent()
			advanceTimeBy(1999)
			runCurrent()
			actions shouldBe emptyList()
			advanceTimeBy(1)
			runCurrent()
			actions shouldBe listOf(SyncPlayAction(SendCommandType.UNPAUSE, 0))
		}
	}

	test("T07 late unpause extrapolates but pause does not") {
		runTest {
			val actions = mutableListOf<SyncPlayAction>()
			val scheduler = SyncPlayCommandScheduler(backgroundScope, { Instant.ofEpochMilli(4000) }, actions::add)
			scheduler.schedule(command(position = 100_000_000))
			runCurrent()
			actions.last().positionTicks shouldBe 120_000_000L
			scheduler.schedule(command(SendCommandType.PAUSE, position = 100_000_000))
			runCurrent()
			actions.last().positionTicks shouldBe 100_000_000L
		}
	}

	test("T11 superseding command and cancel prevent delayed playback") {
		runTest {
			val actions = mutableListOf<SyncPlayAction>()
			val scheduler = SyncPlayCommandScheduler(backgroundScope, { Instant.ofEpochMilli(testScheduler.currentTime) }, actions::add)
			scheduler.schedule(command())
			runCurrent()
			scheduler.schedule(command(SendCommandType.STOP, whenMillis = 1000, position = null))
			runCurrent()
			advanceTimeBy(2500)
			runCurrent()
			actions shouldBe listOf(SyncPlayAction(SendCommandType.STOP, null))
			scheduler.schedule(command(whenMillis = 3000))
			runCurrent()
			scheduler.cancel()
			advanceTimeBy(1000)
			runCurrent()
			actions.size shouldBe 1
		}
	}

	test("T05 does not execute without a synchronized clock") {
		runTest {
			val actions = mutableListOf<SyncPlayAction>()
			val scheduler = SyncPlayCommandScheduler(backgroundScope, { null }, actions::add)
			scheduler.schedule(command()) shouldBe false
			runCurrent()
			actions shouldBe emptyList()
		}
	}

	test("T03 lost clock after scheduling prevents execution") {
		runTest {
			val actions = mutableListOf<SyncPlayAction>()
			var now: Instant? = Instant.EPOCH
			val scheduler = SyncPlayCommandScheduler(backgroundScope, { now }, actions::add)
			scheduler.schedule(command())
			runCurrent()
			now = null
			advanceTimeBy(3000)
			runCurrent()
			actions shouldBe emptyList()
		}
	}

	test("T12 tick arithmetic preserves large positions and saturates overflow") {
		runTest {
			val actions = mutableListOf<SyncPlayAction>()
			val scheduler = SyncPlayCommandScheduler(backgroundScope, { Instant.ofEpochMilli(4000) }, actions::add)
			scheduler.schedule(command(position = 30_000_000_000_000))
			runCurrent()
			actions.last().positionTicks shouldBe 30_000_020_000_000L
			scheduler.schedule(command(position = Long.MAX_VALUE))
			runCurrent()
			actions.last().positionTicks shouldBe Long.MAX_VALUE
		}
	}
	test("T06 a revised clock estimate cannot execute a command early") {
		runTest {
			val actions = mutableListOf<SyncPlayAction>()
			var offset = 0L
			val scheduler = SyncPlayCommandScheduler(
				backgroundScope,
				{ Instant.ofEpochMilli(testScheduler.currentTime + offset) },
				actions::add,
			)
			scheduler.schedule(command())
			runCurrent()
			offset = -100
			advanceTimeBy(2000)
			runCurrent()
			actions shouldBe emptyList()
			advanceTimeBy(100)
			runCurrent()
			actions shouldBe listOf(SyncPlayAction(SendCommandType.UNPAUSE, 0))
		}
	}

	test("T11 a new command invalidates an old deadline even while clock is unavailable") {
		runTest {
			val actions = mutableListOf<SyncPlayAction>()
			var clockAvailable = true
			val scheduler = SyncPlayCommandScheduler(
				backgroundScope,
				{ if (clockAvailable) Instant.ofEpochMilli(testScheduler.currentTime) else null },
				actions::add,
			)
			scheduler.schedule(command())
			runCurrent()
			clockAvailable = false
			scheduler.schedule(command(SendCommandType.PAUSE)) shouldBe false
			clockAvailable = true
			advanceTimeBy(3000)
			runCurrent()
			actions shouldBe emptyList()
		}
	}

})
