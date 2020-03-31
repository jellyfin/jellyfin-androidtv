package org.jellyfin.androidtv.model.trailers.lifter

import org.jellyfin.apiclient.model.entities.MediaUrl
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class YouTubeLifterSupportedURLsTest(private val testURL: String) {
	private val lifter = YouTubeExternalTrailerLifter()
	private val expectedName = "Teppich fliegen, das ist logisch"
	private val expectedThumbnailUrl = "https://i1.ytimg.com/vi/toVK97H4j24/hqdefault.jpg"
	private val expectedPlaybackUrl = "https://www.youtube.com/watch?v=toVK97H4j24"

	private fun getMediaURL() = MediaUrl().apply {
		url = testURL
		name = expectedName
	}

	@Test
	fun shouldAccept() {
		Assert.assertEquals(true, lifter.canLift(getMediaURL()))
	}

	@Test
	fun liftsCorrectly() {
		val lifted = lifter.lift(getMediaURL())
		Assert.assertEquals(expectedName, lifted.name)
		Assert.assertEquals(expectedPlaybackUrl, lifted.playbackURL)
		Assert.assertEquals(expectedThumbnailUrl, lifted.thumbnailURL)
	}

	companion object {
		@JvmStatic
		@Parameterized.Parameters(name = "Input: {0}")
		fun params() = listOf(
			// Default cases of the same video
			arrayOf("https://www.youtube.com/watch?v=toVK97H4j24"),
			arrayOf("http://www.youtube.com/watch?v=toVK97H4j24"),
			arrayOf("www.youtube.com/watch?v=toVK97H4j24"),
			arrayOf("youtube.com/watch?v=toVK97H4j24"),
			arrayOf("https://youtu.be/toVK97H4j24"),
			arrayOf("http://youtu.be/toVK97H4j24"),
			arrayOf("youtu.be/toVK97H4j24"),

			// More special cases with timestamp which will be omitted
			arrayOf("https://www.youtube.com/watch?v=toVK97H4j24&t=122"),
			arrayOf("https://www.youtube.com/watch?t=122&v=toVK97H4j24")
		)
	}
}
