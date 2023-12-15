package org.jellyfin.androidtv.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.preference.UserPreferences
import kotlin.time.Duration.Companion.milliseconds

class ScreensaverViewModel(
	private val userPreferences: UserPreferences,
) : ViewModel() {
	private var timer: Job? = null
	private var locks = 0

	// Preferences

	val inAppEnabled get() = userPreferences[UserPreferences.screensaverInAppEnabled]
	private val timeout get() = userPreferences[UserPreferences.screensaverInAppTimeout].milliseconds

	// State

	private val _visible = MutableStateFlow(false)
	val visible get() = _visible.asStateFlow()

	private val _keepScreenOn = MutableStateFlow(inAppEnabled)
	val keepScreenOn get() = _keepScreenOn.asStateFlow()

	var activityPaused: Boolean = false
		set(value) {
			field = value
			notifyInteraction(true)
		}

	init {
		notifyInteraction(true)
	}

	fun notifyInteraction(canCancel: Boolean) {
		// Cancel pending timer (if any)
		timer?.cancel()

		// Hide when interacted with allowed cancelation or when disabled
		if (_visible.value && (canCancel || !inAppEnabled || activityPaused)) {
			_visible.value = false
		}

		// Create new timer to show screensaver when enabled
		if (inAppEnabled && !activityPaused && locks == 0) {
			timer = viewModelScope.launch {
				delay(timeout)
				_visible.value = true
			}
		}

		// Update KEEP_SCREEN_ON flag value
		_keepScreenOn.value = inAppEnabled || locks > 0
	}

	/**
	 * Create a lock that prevents the screensaver for running until the returned function is called
	 * or the lifecycle is destroyed.
	 *
	 * @return Function to cancel the lock
	 */
	fun addLifecycleLock(lifecycle: Lifecycle): () -> Unit {
		if (lifecycle.currentState == Lifecycle.State.DESTROYED) return {}

		val lock = ScreensaverLock(lifecycle)
		lock.activate()
		return lock::cancel
	}

	private inner class ScreensaverLock(private val lifecycle: Lifecycle) : LifecycleEventObserver {
		private var active: Boolean = false

		override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
			if (event == Lifecycle.Event.ON_DESTROY) cancel()
		}

		fun activate() {
			if (active) return
			lifecycle.addObserver(this)
			locks++
			notifyInteraction(true)
			active = true
		}

		fun cancel() {
			if (!active) return
			locks--
			lifecycle.removeObserver(this)
			notifyInteraction(false)
			active = false
		}
	}
}
