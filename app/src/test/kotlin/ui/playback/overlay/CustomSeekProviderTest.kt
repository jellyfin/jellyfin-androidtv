package org.jellyfin.androidtv.ui.playback.overlay

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomSeekProviderTest {
	@Test
	fun testGetSeekPositions_withSimpleDuration() {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter>()
		every { videoPlayerAdapter.canSeek() } returns true
		every { videoPlayerAdapter.duration } returns 90000L
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter)
		val expected = longArrayOf(0L, 30000L, 60000L, 90000L)
		val actual = customSeekProvider.seekPositions
		assertArrayEquals(expected, actual)
	}

	@Test
	fun testGetSeekPositions_withOddDuration() {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter>()
		every { videoPlayerAdapter.canSeek() } returns true
		every { videoPlayerAdapter.duration } returns 130000L
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter)
		val expected = longArrayOf(0L, 30000, 60000, 90000, 120000, 130000)
		val actual = customSeekProvider.seekPositions
		assertArrayEquals(expected, actual)
	}

	@Test
	fun testGetSeekPositions_withSeekDisabled() {
		val videoPlayerAdapter = mockk<VideoPlayerAdapter>()
		every { videoPlayerAdapter.canSeek() } returns false
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter)
		val actual = customSeekProvider.seekPositions
		assertEquals(0, actual.size.toLong())
	}
}
