package org.jellyfin.androidtv.ui.playback

import io.mockk.*
import org.junit.After
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.exp

class VideoSpeedControllerTest {
	@After
	fun tearDown() {
		// Always reset the "user selected" speed back to default
		VideoSpeedController.resetPreviousSpeedToDefault()
	}

	@Test
	fun testSpeedSteps() {
		val speedSteps = VideoSpeedController.Companion.SpeedSteps.values()
		val expectedStep = 0.25
		var i = 1
		speedSteps.forEach { v ->
			assertEquals(i * expectedStep, v.value, 0.001)
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
	fun testSetNewSpeed(){
		val mockController = mockk<PlaybackController>(relaxed = true)
		val controller = VideoSpeedController(mockController)
		val expected = VideoSpeedController.Companion.SpeedSteps.SPEED_1_25
		controller.setNewSpeed(expected)
		assertEquals(expected, controller.getCurrentSpeed())
	}

	@Test
	fun testSetNewSpeedSetsOnManager() {
		val mockController = mockk<PlaybackController>()
		val slot = slot<Double>()
		justRun { mockController.setPlaybackSpeed(capture(slot)) }

		val controller = VideoSpeedController(mockController)
		val expected = VideoSpeedController.Companion.SpeedSteps.SPEED_1_75
		controller.setNewSpeed(expected)

		verify { mockController.setPlaybackSpeed(any()) }
		assertEquals(expected.value, slot.captured, 0.0001)
	}

	@Test
	fun testResetPreviousSpeedToDefault() {
		val mockController = mockk<PlaybackController>(relaxed = true)
		VideoSpeedController(mockController).setNewSpeed(VideoSpeedController.Companion.SpeedSteps.SPEED_2_00)
		VideoSpeedController.resetPreviousSpeedToDefault()

		val slot = slot<Double>()
		justRun { mockController.setPlaybackSpeed(capture(slot)) }
		VideoSpeedController(mockController)

		verify { mockController.setPlaybackSpeed(any()) }
		assertEquals(VideoSpeedController.Companion.SpeedSteps.SPEED_1_00.value, slot.captured, 0.0001)
	}


	@Test
	fun testControllerPreservesMostRecentlySelectedSpeedConstructingNew() {
		var lastSetSpeed = 1.0
		val speeds = VideoSpeedController.Companion.SpeedSteps.values()

		speeds.forEach { newSpeed ->
			val mockController = mockk<PlaybackController>()
			val slot = slot<Double>()
			justRun { mockController.setPlaybackSpeed(capture(slot)) }

			val controller = VideoSpeedController(mockController)

			verify { mockController.setPlaybackSpeed(any()) }
			assertEquals(lastSetSpeed, slot.captured, 0.001)

			controller.setNewSpeed(newSpeed)
			lastSetSpeed = newSpeed.value
		}
	}

}
