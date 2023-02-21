package org.jellyfin.androidtv.ui

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
	val enabled get() = userPreferences[UserPreferences.screensaverInAppEnabled]
	private val timeout get() = userPreferences[UserPreferences.screensaverInAppTimeout].milliseconds

	private val _visible = MutableStateFlow(false)
	val visible get() = _visible.asStateFlow()
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
		if (_visible.value && (canCancel || !enabled || activityPaused)) {
			_visible.value = false
		}

		// Create new timer to show screensaver when enabled
		if (enabled && !activityPaused) {
			timer = viewModelScope.launch {
				delay(timeout)
				_visible.value = true
			}
		}
	}
}
