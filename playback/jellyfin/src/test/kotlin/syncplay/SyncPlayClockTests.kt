package org.jellyfin.playback.jellyfin.syncplay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SyncPlayClockTests : FunSpec({
	test("server processing time is excluded from ping and offset uses all four timestamps") {
		val time = ClockTime()
		val clock = time.clock()
		val request = clock.beginMeasurement()
		time.advance(140)

		clock.completeMeasurement(request, time.epoch + 1050, time.epoch + 1090) shouldBe true
		clock.isSynchronized shouldBe true
		clock.offsetMillis shouldBe 1000.0
		clock.pingMillis shouldBe 50
		clock.nowMillis() shouldBe time.epoch + 1140
	}

	test("the lowest latency sample is retained until it leaves the eight sample window") {
		val time = ClockTime()
		val clock = time.clock()
		fun measure(offset: Long, delay: Long) {
			val sent = time.epoch + time.elapsedMillis
			val request = clock.beginMeasurement()
			time.advance(delay)
			clock.completeMeasurement(request, sent + delay / 2 + offset, sent + delay / 2 + offset) shouldBe true
		}

		measure(1000, 20)
		repeat(7) { measure(2000, 100) }
		clock.offsetMillis shouldBe 1000.0
		clock.pingMillis shouldBe 10
		measure(2000, 100)
		clock.offsetMillis shouldBe 2000.0
		clock.pingMillis shouldBe 50
	}

	test("device clock jumps cannot move server time or a scheduled command") {
		val time = ClockTime()
		val clock = time.clock()
		val request = clock.beginMeasurement()
		time.advance(100)
		clock.completeMeasurement(request, time.epoch + 1050, time.epoch + 1050) shouldBe true
		val scheduledTime = clock.nowMillis() + 1000

		time.wallMillis += 3_600_000
		time.advance(250)
		clock.nowMillis() shouldBe time.epoch + 1350
		clock.delayUntil(scheduledTime) shouldBe 750
		time.wallMillis -= 7_200_000
		time.advance(1000)
		clock.delayUntil(scheduledTime) shouldBe 0
		clock.monotonicMillis() shouldBe 1350
	}

	test("wall time changes during measurement do not corrupt the estimate") {
		val time = ClockTime()
		val clock = time.clock()
		val request = clock.beginMeasurement()
		time.wallMillis += 3_600_000
		time.advance(100)
		clock.completeMeasurement(request, time.epoch + 1050, time.epoch + 1050) shouldBe true
		clock.offsetMillis shouldBe 1000.0
		clock.pingMillis shouldBe 50
	}

	test("invalid timestamps and requests from before reset cannot replace a measurement") {
		val time = ClockTime()
		val clock = time.clock()
		val request = clock.beginMeasurement()
		time.advance(100)
		clock.completeMeasurement(request, time.epoch + 200, time.epoch + 100) shouldBe false
		clock.completeMeasurement(request, time.epoch, time.epoch + 200) shouldBe false
		clock.isSynchronized shouldBe false
		clock.completeMeasurement(request, time.epoch + 1050, time.epoch + 1050) shouldBe true
		clock.reset()
		clock.isSynchronized shouldBe false
		clock.pingMillis shouldBe 0
		clock.completeMeasurement(request, time.epoch + 1050, time.epoch + 1050) shouldBe false
	}

	test("ping rounds one way delay instead of reporting the round trip") {
		val time = ClockTime()
		val clock = time.clock()
		val request = clock.beginMeasurement()
		time.advance(101)
		clock.completeMeasurement(request, time.epoch + 1050, time.epoch + 1050) shouldBe true
		clock.pingMillis shouldBe 51
	}
})

private class ClockTime {
	val epoch = 1_700_000_000_000L
	var wallMillis = epoch
	var elapsedMillis = 0L

	fun clock() = SyncPlayClock({ wallMillis }, { elapsedMillis * 1_000_000 })

	fun advance(millis: Long) {
		elapsedMillis += millis
		wallMillis += millis
	}
}
