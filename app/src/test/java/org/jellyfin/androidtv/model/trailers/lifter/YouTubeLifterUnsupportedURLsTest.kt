package org.jellyfin.androidtv.model.trailers.lifter

import org.jellyfin.apiclient.model.entities.MediaUrl
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized


@RunWith(Parameterized::class)
class YouTubeLifterUnsupportedURLsTest(private val testURL: String) {
	private val lifter = YouTubeTrailerLifter()

	private fun getMediaURL() = MediaUrl().apply {
		url = testURL
	}

	@Test
	fun shouldIgnore() {
		Assert.assertEquals(lifter.canLift(getMediaURL()), false)
	}

	companion object {
		@JvmStatic
		@Parameterized.Parameters(name = "Input: {0}")
		fun params() = listOf(
			arrayOf("https://test.com"),
			arrayOf("http://test.com"),
			arrayOf("test.com"),
			arrayOf("https://youtube.test.com"),
			arrayOf("https://test.youtube"),
			arrayOf("https://youtube")
		)
	}
}

