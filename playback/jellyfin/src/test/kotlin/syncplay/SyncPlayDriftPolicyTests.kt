package org.jellyfin.playback.jellyfin.syncplay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SyncPlayDriftPolicyTests : FunSpec({
	val policy = SyncPlayDriftPolicy()

	test("ongoing correction is disabled by default as in Jellyfin Web") {
		policy.correction(5000.0, supportsPlaybackRate = true) shouldBe SyncPlayCorrection.None
		policy.correctionIntervalMillis shouldBe 1500
		policy.unpauseGracePeriodMillis shouldBe 1500
	}

	test("small drift is tolerated and speed correction starts at sixty milliseconds") {
		policy.correction(59.0, true, enabled = true) shouldBe SyncPlayCorrection.None
		policy.correction(-59.0, true, enabled = true) shouldBe SyncPlayCorrection.None
		policy.correction(60.0, true, enabled = true) shouldBe SyncPlayCorrection.ChangeSpeed(1.06f, 1000)
		policy.correction(-60.0, true, enabled = true) shouldBe SyncPlayCorrection.ChangeSpeed(0.94f, 1000)
	}

	test("a client behind the group speeds up for one second before the skip threshold") {
		policy.correction(1500.0, true, enabled = true) shouldBe SyncPlayCorrection.ChangeSpeed(2.5f, 1000)
		policy.correction(2999.0, true, enabled = true).shouldBeInstanceOf<SyncPlayCorrection.ChangeSpeed>()
		policy.correction(3000.0, true, enabled = true) shouldBe SyncPlayCorrection.Seek
		policy.correction(-3000.0, true, enabled = true) shouldBe SyncPlayCorrection.Seek
	}

	test("a client ahead of the group uses Web's longer duration and positive minimum speed") {
		val correction = policy.correction(-1600.0, true, enabled = true).shouldBeInstanceOf<SyncPlayCorrection.ChangeSpeed>()
		correction.rate shouldBe (0.2f plusOrMinus 0.00001f)
		correction.durationMillis shouldBe 2000
		policy.correction(-200.0, true, enabled = true) shouldBe SyncPlayCorrection.ChangeSpeed(0.2f, 250)
	}

	test("players without variable speed seek only after four hundred milliseconds") {
		policy.correction(399.0, false, enabled = true) shouldBe SyncPlayCorrection.None
		policy.correction(-399.0, false, enabled = true) shouldBe SyncPlayCorrection.None
		policy.correction(400.0, false, enabled = true) shouldBe SyncPlayCorrection.Seek
		policy.correction(-400.0, false, enabled = true) shouldBe SyncPlayCorrection.Seek
	}

	test("invalid position measurements cannot cause player corrections") {
		policy.correction(Double.NaN, true, enabled = true) shouldBe SyncPlayCorrection.None
		policy.correction(Double.POSITIVE_INFINITY, true, enabled = true) shouldBe SyncPlayCorrection.None
		policy.correction(Double.NEGATIVE_INFINITY, true, enabled = true) shouldBe SyncPlayCorrection.None
	}
})
