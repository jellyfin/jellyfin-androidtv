package org.jellyfin.androidtv.ui.playback.overlay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class CustomSeekProviderTests : FunSpec({
	test("CustomSeekProvider.seekPositions with simple duration from beginning") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns true
			every { duration } returns 30000L
			every { currentPosition } returns 0L
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter, mockk(), mockk(), mockk(), false, 10_000)

		customSeekProvider.seekPositions shouldBe longArrayOf(0L, 10000L, 20000L, 30000L)
	}

	test("CustomSeekProvider.seekPositions with simple duration from end") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns true
			every { duration } returns 30000L
			every { currentPosition } returns 30000L
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter, mockk(), mockk(), mockk(), false, 10_000)

		customSeekProvider.seekPositions shouldBe longArrayOf(0L, 10000L, 20000L, 30000L)
	}

	test("CustomSeekProvider.seekPositions with odd duration from beginning") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns true
			every { duration } returns 45000L
			every { currentPosition } returns 0L
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter, mockk(), mockk(), mockk(), false, 10_000)

		customSeekProvider.seekPositions shouldBe longArrayOf(0L, 10000, 20000, 30000, 40000, 45000)
	}

	test("CustomSeekProvider.seekPositions with seek disabled") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns false
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter, mockk(), mockk(), mockk(), false, 10_000)

		customSeekProvider.seekPositions.size shouldBe 0
	}

	test("CustomSeekProvider.seekPositions from a mid-video position") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns true
			every { duration } returns 60_000L
			every { currentPosition } returns 32_500L
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter, mockk(), mockk(), mockk(), false, 10_000)

		customSeekProvider.seekPositions shouldBe longArrayOf(
			0, 2500, 12500, 22500, 32500, 42500, 52500, 60000
		)
	}

	test("CustomSeekProvider.seekPositions from a near-beginning position") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns true
			every { duration } returns 60_000L
			every { currentPosition } returns 10_500L
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter, mockk(), mockk(), mockk(), false, 10_000)

		customSeekProvider.seekPositions shouldBe longArrayOf(
			0, 500, 10500, 20500, 30500, 40500, 50500, 60000
		)
	}

	test("CustomSeekProvider.seekPositions from a within first second position") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns true
			every { duration } returns 60_000L
			every { currentPosition } returns 500L
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter, mockk(), mockk(), mockk(), false, 10_000)

		customSeekProvider.seekPositions shouldBe longArrayOf(
			0, 500, 10500, 20500, 30500, 40500, 50500, 60000
		)
	}

	test("CustomSeekProvider.seekPositions from a near-end position") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns true
			every { duration } returns 60_000L
			every { currentPosition } returns 49_500L
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter, mockk(), mockk(), mockk(), false, 10_000)

		customSeekProvider.seekPositions shouldBe longArrayOf(
			0, 9500, 19500, 29500, 39500, 49500, 59500, 60000
		)
	}

	test("CustomSeekProvider.seekPositions from within a last second position") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns true
			every { duration } returns 60_000L
			every { currentPosition } returns 59_500L
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter, mockk(), mockk(), mockk(), false, 10_000)

		customSeekProvider.seekPositions shouldBe longArrayOf(
			0, 9500, 19500, 29500, 39500, 49500, 59500, 60000
		)
	}

	test("CustomSeekProvider.seekPositions with zero duration") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns true
			every { duration } returns 0L
			every { currentPosition } returns 0L
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter, mockk(), mockk(), mockk(), false, 10_000)

		customSeekProvider.seekPositions shouldBe longArrayOf(0)
	}
})

