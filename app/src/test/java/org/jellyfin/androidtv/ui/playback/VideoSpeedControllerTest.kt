package org.jellyfin.androidtv.ui.playback

import io.mockk.*
import org.junit.After
import org.junit.Test
import org.junit.Assert.*

class VideoSpeedControllerTest {
	@After
	fun tearDown() {
		// Always reset the "user selected" speed back to default
		VideoSpeedController.mostRecentSpeed = 1.0
	}

	@Test
	fun testSpeedSteps() {
		val speedSteps = VideoSpeedController.speedSteps
		val expectedStep = 0.25
		var i = 1
		speedSteps.forEach { v ->
			assertEquals(i * expectedStep, v, 0.001)
			i += 1
		}
	}

	@Test
	fun testControllerSpeedIsOneByDefault() {
		val mockController = mockk<PlaybackController>()
		val slot = slot<Double>()
		justRun { mockController.setPlaybackSpeed(capture(slot)) }

		VideoSpeedController(mockController)

		verify { mockController.setPlaybackSpeed(any()) }
		assertEquals(1.0, slot.captured, 0.001)
	}

	@Test
	fun testSetNewSpeed() {
		val mockController = mockk<PlaybackController>()
		val slot = slot<Double>()
		justRun { mockController.setPlaybackSpeed(capture(slot)) }

		val controller = VideoSpeedController(mockController)
		val expected = 0.567
		controller.setNewSpeed(expected)

		verify { mockController.setPlaybackSpeed(any()) }
		assertEquals(expected, slot.captured, 0.0001)
	}

	@Test
	fun testControllerPreservesMostRecentlySelectedSpeedConstructingNew() {
		var lastSetSpeed = 1.0

		for (i in 1..5) {
			val mockController = mockk<PlaybackController>()
			val slot = slot<Double>()
			justRun { mockController.setPlaybackSpeed(capture(slot)) }

			val controller = VideoSpeedController(mockController)

			verify { mockController.setPlaybackSpeed(any()) }
			assertEquals(lastSetSpeed, slot.captured, 0.001)

			val newSpeed = i.toDouble() / 10
			controller.setNewSpeed(newSpeed)
			lastSetSpeed = newSpeed
		}
	}

}
