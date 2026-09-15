package org.jellyfin.playback.jellyfin.syncplay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class SyncPlayClockTests : FunSpec({
	test("T01 estimates server offset and one-way ping excluding server processing") {
		val clock = SyncPlayClock()
		clock.record(1000, 1120, 1130, 1050, 500, 550) shouldBe true
		clock.pingMillis shouldBe 20L
		clock.serverTime(1500) shouldBe Instant.ofEpochMilli(2100)
	}

	test("T02 selects lowest delay in a bounded window and expires old samples") {
		val clock = SyncPlayClock()
		clock.record(1000, 1110, 1110, 1020, 0, 20)
		repeat(7) { clock.record(1000, 1300, 1300, 1200, 0, 200) }
		clock.pingMillis shouldBe 10L
		clock.serverTime(200) shouldBe Instant.ofEpochMilli(1300)
		clock.record(1000, 1300, 1300, 1200, 0, 200)
		clock.pingMillis shouldBe 100L
		clock.serverTime(200) shouldBe Instant.ofEpochMilli(1400)
	}

	test("T03 rejects invalid measurements without becoming ready") {
		val clock = SyncPlayClock()
		clock.record(1000, 1100, 1200, 1050, 0, 50) shouldBe false
		clock.record(1000, 1200, 1100, 1050, 0, 50) shouldBe false
		clock.record(1000, 1100, 1100, 1050, 50, 0) shouldBe false
		clock.serverTime(100) shouldBe null
	}

	test("T03 wall clock jump invalidates old measurement") {
		val clock = SyncPlayClock()
		clock.record(1000, 1100, 1100, 1000, 0, 0)
		clock.record(1000, 1100, 1100, 5000, 0, 50) shouldBe false
		clock.serverTime(50) shouldBe null
	}

	test("T05 reset removes readiness and latency") {
		val clock = SyncPlayClock()
		clock.record(1000, 1100, 1100, 1000, 0, 0)
		clock.reset()
		clock.serverTime(0) shouldBe null
		clock.pingMillis shouldBe null
	}

	test("clock advances from monotonic anchor and handles a negative offset") {
		val clock = SyncPlayClock()
		clock.record(2000, 1910, 1910, 2020, 0, 20)
		clock.serverTime(120) shouldBe Instant.ofEpochMilli(2020)
	}
})
