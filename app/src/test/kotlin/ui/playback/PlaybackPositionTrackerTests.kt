package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PlaybackPositionTrackerTests : FunSpec({
	test("recoverable position uses last stable player position instead of stale session resume seek") {
		val tracker = PlaybackPositionTracker()

		tracker.reset(600_000)
		tracker.updateFromPlayerPosition(1_860_000)

		tracker.getRecoverablePosition(600_000, 600_000, false) shouldBe 1_860_000
	}

	test("pending seek position wins while a seek is in flight") {
		val tracker = PlaybackPositionTracker()

		tracker.reset(1_860_000)

		tracker.getRecoverablePosition(1_860_000, 100_000, true) shouldBe 100_000
	}

	test("pending seek to zero is recoverable while a seek is in flight") {
		val tracker = PlaybackPositionTracker()

		tracker.reset(1_860_000)
		tracker.updateFromSeekPosition(0)

		tracker.getRecoverablePosition(1_860_000, 0, true) shouldBe 0
	}

	test("player reported zero does not replace a positive stable position") {
		val tracker = PlaybackPositionTracker()

		tracker.reset(600_000)
		tracker.updateFromPlayerPosition(1_860_000)
		tracker.updateFromPlayerPosition(0)

		tracker.getRecoverablePosition(0, 600_000, false) shouldBe 1_860_000
	}

	test("recoverable position falls back to current then pending seek") {
		val tracker = PlaybackPositionTracker()

		tracker.reset(0)
		tracker.getRecoverablePosition(42_000, -1, false) shouldBe 42_000

		val newTracker = PlaybackPositionTracker()
		newTracker.getRecoverablePosition(0, 600_000, false) shouldBe 600_000
	}
})
