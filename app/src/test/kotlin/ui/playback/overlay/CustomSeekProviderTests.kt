package org.jellyfin.androidtv.ui.playback.overlay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class CustomSeekProviderTests : FunSpec({
	test("CustomSeekProvider.seekPositions with simple duration") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns true
			every { duration } returns 30000L
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter, mockk(), mockk(), mockk(), false, 10_000)

		customSeekProvider.seekPositions shouldBe arrayOf(0L, 10000L, 20000L, 30000L)
	}

	test("CustomSeekProvider.seekPositions with odd duration") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns true
			every { duration } returns 45000L
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter, mockk(), mockk(), mockk(), false, 10_000)

		customSeekProvider.seekPositions shouldBe arrayOf(0L, 10000, 20000, 30000, 40000, 45000)
	}

	test("CustomSeekProvider.seekPositions with seek disabled") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns false
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter, mockk(), mockk(), mockk(), false, 10_000)

		customSeekProvider.seekPositions.size shouldBe 0
	}
})
