package org.jellyfin.androidtv.ui.playback.overlay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class CustomSeekProviderTests : FunSpec({
	test("CustomSeekProvider.seekPositions with simple duration") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns true
			every { duration } returns 90000L
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter)

		customSeekProvider.seekPositions shouldBe arrayOf(0L, 30000L, 60000L, 90000L)
	}

	test("CustomSeekProvider.seekPositions with odd duration") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns true
			every { duration } returns 130000L
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter)

		customSeekProvider.seekPositions shouldBe arrayOf(0L, 30000, 60000, 90000, 120000, 130000)
	}

	test("CustomSeekProvider.seekPositions with seek disabled") {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter> {
			every { canSeek() } returns false
		}
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter)

		customSeekProvider.seekPositions.size shouldBe 0
	}
})
