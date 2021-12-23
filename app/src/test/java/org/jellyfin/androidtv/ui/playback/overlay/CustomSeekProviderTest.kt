package org.jellyfin.androidtv.ui.playback.overlay

import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class CustomSeekProviderTest {
	@Test
	fun testGetSeekPositions_withSimpleDuration() {
		val videoPlayerAdapter = Mockito.mock(
			VideoPlayerAdapter::class.java
		)
		Mockito.`when`(videoPlayerAdapter.canSeek()).thenReturn(true)
		Mockito.`when`(videoPlayerAdapter.duration).thenReturn(90000L)
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter)
		val expected = longArrayOf(0L, 30000L, 60000L, 90000L)
		val actual = customSeekProvider.seekPositions
		Assert.assertArrayEquals(expected, actual)
	}

	@Test
	fun testGetSeekPositions_withOddDuration() {
		val videoPlayerAdapter = Mockito.mock(
			VideoPlayerAdapter::class.java
		)
		Mockito.`when`(videoPlayerAdapter.canSeek()).thenReturn(true)
		Mockito.`when`(videoPlayerAdapter.duration).thenReturn(130000L)
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter)
		val expected = longArrayOf(0L, 30000, 60000, 90000, 120000, 130000)
		val actual = customSeekProvider.seekPositions
		Assert.assertArrayEquals(expected, actual)
	}

	@Test
	fun testGetSeekPositions_withSeekDisabled() {
		val videoPlayerAdapter = Mockito.mock(
			VideoPlayerAdapter::class.java
		)
		Mockito.`when`(videoPlayerAdapter.canSeek()).thenReturn(false)
		val customSeekProvider = CustomSeekProvider(videoPlayerAdapter)
		val actual = customSeekProvider.seekPositions
		Assert.assertEquals(0, actual.size.toLong())
	}
}
