package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class VideoSpeedControllerTests : FunSpec({
	afterTest {
		// Always reset the "user selected" speed back to default
		VideoSpeedController(mockk(relaxed = true)).resetSpeedToDefault()
	}

	test("VideoSpeedController.SpeedSteps uses intervals of 0.25") {
		VideoSpeedController.SpeedSteps.entries.forEachIndexed { i, v ->
			v.speed.shouldBe((i + 1) * 0.25f plusOrMinus 1.0E-4F)
		}
	}

	test("VideoSpeedController speed is 1 by default") {
		val mockController = mockk<PlaybackController>(relaxed = true)
		val slot = slot<Float>()
		justRun { mockController.setPlaybackSpeed(capture(slot)) }

		VideoSpeedController(mockController)

		verify { mockController.setPlaybackSpeed(any()) }
		slot.captured shouldBe (1.0f plusOrMinus 1.0E-4F)
	}

	test("VideoSpeedController.currentSpeed getter returns set value") {
		val mockController = mockk<PlaybackController>(relaxed = true)
		val controller = VideoSpeedController(mockController)

		val expected = VideoSpeedController.SpeedSteps.SPEED_1_25
		controller.currentSpeed = expected

		controller.currentSpeed shouldBe expected
	}

	test("VideoSpeedController.currentSpeed updates the speed in the controller") {
		val mockController = mockk<PlaybackController>(relaxed = true)
		val slot = slot<Float>()
		justRun { mockController.setPlaybackSpeed(capture(slot)) }

		val controller = VideoSpeedController(mockController)
		val expected = VideoSpeedController.SpeedSteps.SPEED_1_75
		controller.currentSpeed = expected

		verify { mockController.setPlaybackSpeed(any()) }
		slot.captured shouldBe (expected.speed plusOrMinus 1.0E-4F)
	}

	test("VideoSpeedController.resetSpeedToDefault() works correctly") {
		val playbackController = mockk<PlaybackController>(relaxed = true)
		val videoController = VideoSpeedController(playbackController)

		videoController.currentSpeed = VideoSpeedController.SpeedSteps.SPEED_2_00
		videoController.resetSpeedToDefault()

		val slot = slot<Float>()
		justRun { playbackController.setPlaybackSpeed(capture(slot)) }
		VideoSpeedController(playbackController)

		verify { playbackController.setPlaybackSpeed(any()) }
		slot.captured shouldBe (VideoSpeedController.SpeedSteps.SPEED_1_00.speed plusOrMinus 1.0E-4F)
	}

	test("VideoSpeedController remembers previous instance speed value") {
		var lastSetSpeed = 1.0f

		VideoSpeedController.SpeedSteps.entries.forEach { newSpeed ->
			val mockController = mockk<PlaybackController>(relaxed = true)
			val slot = slot<Float>()
			justRun { mockController.setPlaybackSpeed(capture(slot)) }

			val controller = VideoSpeedController(mockController)

			verify { mockController.setPlaybackSpeed(any()) }
			slot.captured shouldBe (lastSetSpeed plusOrMinus 1.0E-4F)

			controller.currentSpeed = newSpeed
			lastSetSpeed = newSpeed.speed
		}
	}

	test("VideoSpeedController.currentSpeed always sets the speed to 1 for LiveTV") {
		// Since handling live TV is more complex, we will simply reset the playback
		// speed to 1 so we can't out-run the current buffer.

		// Assume the user has pre-set their speed
		VideoSpeedController(mockk(relaxed = true)).currentSpeed = VideoSpeedController.SpeedSteps.SPEED_2_00

		// Then they switch to live-tv
		val mockController = mockk<PlaybackController>(relaxed = true) {
			every { isLiveTv } returns true
		}
		val speedController = VideoSpeedController(mockController)

		verify { mockController.setPlaybackSpeed(1.0f) }
		speedController.currentSpeed shouldBe VideoSpeedController.SpeedSteps.SPEED_1_00

		// Try to set it back to other values should be ignored
		speedController.currentSpeed = VideoSpeedController.SpeedSteps.SPEED_2_00

		verify { mockController.setPlaybackSpeed(1.0f) }
		speedController.currentSpeed shouldBe VideoSpeedController.SpeedSteps.SPEED_1_00
	}

	test("VideoSpeedController.currentSpeed always sets the requested speed when LiveTV is off") {
		val mockController = mockk<PlaybackController>(relaxed = true) {
			every { isLiveTv } returns false
		}
		val speedController = VideoSpeedController(mockController)

		verify { mockController.setPlaybackSpeed(1.0f) }
		speedController.currentSpeed shouldBe VideoSpeedController.SpeedSteps.SPEED_1_00

		speedController.currentSpeed = VideoSpeedController.SpeedSteps.SPEED_2_00

		verify { mockController.setPlaybackSpeed(2.0f) }
		speedController.currentSpeed shouldBe VideoSpeedController.SpeedSteps.SPEED_2_00
	}
})
