package org.jellyfin.androidtv.ui

import androidx.lifecycle.Lifecycle
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jellyfin.androidtv.preference.UserPreferences
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class InteractionTrackerViewModelTests : FunSpec({
	val dispatcher = StandardTestDispatcher()

	beforeTest { Dispatchers.setMain(dispatcher) }
	afterTest { Dispatchers.resetMain() }

	fun createViewModel(
		timeout: Duration = 5.minutes,
		sleepTimeout: Duration = Duration.INFINITE,
	): InteractionTrackerViewModel {
		val userPreferences = mockk<UserPreferences>()
		every { userPreferences[UserPreferences.screensaverInAppEnabled] } returns true
		every { userPreferences[UserPreferences.screensaverInAppTimeout] } returns timeout.inWholeMilliseconds
		every { userPreferences[UserPreferences.screensaverInAppSleepTimeout] } returns sleepTimeout.inWholeMilliseconds

		return InteractionTrackerViewModel(userPreferences, mockk(relaxed = true))
	}

	test("InteractionTrackerViewModel.keepScreenOn stays set when the sleep timeout is never") {
		runTest(dispatcher) {
			val viewModel = createViewModel(sleepTimeout = Duration.INFINITE)

			advanceTimeBy(8.hours)

			viewModel.visible.value shouldBe true
			viewModel.keepScreenOn.value shouldBe true
		}
	}

	test("InteractionTrackerViewModel.keepScreenOn clears with the screensaver when the sleep timeout is immediate") {
		runTest(dispatcher) {
			val viewModel = createViewModel(timeout = 5.minutes, sleepTimeout = Duration.ZERO)

			// The screen is still held while the app is merely idle
			advanceTimeBy(4.minutes)
			viewModel.visible.value shouldBe false
			viewModel.keepScreenOn.value shouldBe true

			advanceTimeBy(1.minutes + 1.milliseconds)
			viewModel.visible.value shouldBe true
			viewModel.keepScreenOn.value shouldBe false
		}
	}

	test("InteractionTrackerViewModel.keepScreenOn clears only once the sleep timeout elapses") {
		runTest(dispatcher) {
			val viewModel = createViewModel(timeout = 5.minutes, sleepTimeout = 30.minutes)

			advanceTimeBy(5.minutes + 1.milliseconds)
			viewModel.visible.value shouldBe true
			viewModel.keepScreenOn.value shouldBe true

			// Not yet: the sleep timeout is measured from the moment the screensaver became visible
			advanceTimeBy(29.minutes)
			viewModel.keepScreenOn.value shouldBe true

			advanceTimeBy(1.minutes)
			viewModel.keepScreenOn.value shouldBe false
		}
	}

	test("InteractionTrackerViewModel.visible stays set after the screen is released") {
		runTest(dispatcher) {
			val viewModel = createViewModel(timeout = 5.minutes, sleepTimeout = 30.minutes)

			advanceUntilIdle()

			viewModel.keepScreenOn.value shouldBe false
			viewModel.visible.value shouldBe true
		}
	}

	test("InteractionTrackerViewModel.keepScreenOn is restored by an interaction") {
		runTest(dispatcher) {
			val viewModel = createViewModel(timeout = 5.minutes, sleepTimeout = 30.minutes)

			advanceUntilIdle()
			viewModel.keepScreenOn.value shouldBe false

			viewModel.notifyInteraction(canCancel = true, userInitiated = true)

			viewModel.keepScreenOn.value shouldBe true
			viewModel.visible.value shouldBe false
		}
	}

	test("InteractionTrackerViewModel.keepScreenOn stays set while a lifecycle lock is held") {
		runTest(dispatcher) {
			val viewModel = createViewModel(timeout = 5.minutes, sleepTimeout = 30.minutes)
			val lifecycle = mockk<Lifecycle>(relaxed = true)
			every { lifecycle.currentState } returns Lifecycle.State.RESUMED

			viewModel.addLifecycleLock(lifecycle)
			advanceTimeBy(8.hours)

			viewModel.visible.value shouldBe false
			viewModel.keepScreenOn.value shouldBe true
		}
	}
})
