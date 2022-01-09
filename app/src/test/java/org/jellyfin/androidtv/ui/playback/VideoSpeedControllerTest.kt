package org.jellyfin.androidtv.ui.playback

import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoSpeedControllerTest {
	@After
	fun tearDown() {
		// Always reset the "user selected" speed back to default
		VideoSpeedController(mockk(relaxed = true)).resetSpeedToDefault()
	}

	@Test
	fun testSpeedSteps() {
		val speedSteps = VideoSpeedController.SpeedSteps.values()
		val expectedStep = 0.25
		var i = 1
		speedSteps.forEach { v ->
			assertEquals(i * expectedStep, v.speed, 0.001)
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
		val mockController = mockk<PlaybackController>(relaxed = true)
		val controller = VideoSpeedController(mockController)
		val expected = VideoSpeedController.SpeedSteps.SPEED_1_25
		controller.currentSpeed = expected
		assertEquals(expected, controller.currentSpeed)
	}

	@Test
	fun testSetNewSpeedSetsOnManager() {
		val mockController = mockk<PlaybackController>()
		val slot = slot<Double>()
		justRun { mockController.setPlaybackSpeed(capture(slot)) }

		val controller = VideoSpeedController(mockController)
		val expected = VideoSpeedController.SpeedSteps.SPEED_1_75
		controller.currentSpeed = expected

		verify { mockController.setPlaybackSpeed(any()) }
		assertEquals(expected.speed, slot.captured, 0.0001)
	}

	@Test
	fun testResetPreviousSpeedToDefault() {
		val playbackController = mockk<PlaybackController>(relaxed = true)
		val videoController = VideoSpeedController(playbackController)

		videoController.currentSpeed = VideoSpeedController.SpeedSteps.SPEED_2_00
		videoController.resetSpeedToDefault()

		val slot = slot<Double>()
		justRun { playbackController.setPlaybackSpeed(capture(slot)) }
		VideoSpeedController(playbackController)

		verify { playbackController.setPlaybackSpeed(any()) }
		assertEquals(
			VideoSpeedController.SpeedSteps.SPEED_1_00.speed,
			slot.captured,
			0.0001
		)
	}


	@Test
	fun testControllerPreservesMostRecentlySelectedSpeedConstructingNew() {
		var lastSetSpeed = 1.0
		val speeds = VideoSpeedController.SpeedSteps.values()

		speeds.forEach { newSpeed ->
			val mockController = mockk<PlaybackController>()
			val slot = slot<Double>()
			justRun { mockController.setPlaybackSpeed(capture(slot)) }

			val controller = VideoSpeedController(mockController)

			verify { mockController.setPlaybackSpeed(any()) }
			assertEquals(lastSetSpeed, slot.captured, 0.001)

			controller.currentSpeed = newSpeed
			lastSetSpeed = newSpeed.speed
		}
	}

}
