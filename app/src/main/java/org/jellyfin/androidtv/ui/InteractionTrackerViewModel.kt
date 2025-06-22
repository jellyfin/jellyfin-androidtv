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
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.ui.playback.stillwatching.StillWatchingPresetConfigs
import org.jellyfin.androidtv.ui.playback.stillwatching.StillWatchingStates
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import kotlin.time.Duration.Companion.milliseconds

class InteractionTrackerViewModel(
	private val userPreferences: UserPreferences,
	private val playbackControllerContainer: PlaybackControllerContainer
) : ViewModel() {
	// Screensaver vars

	private var timer: Job? = null
	private var locks = 0

	// Still Watching vars

	private var isWatchingEpisodes = false
	private var episodeCount = 0
	private var watchTime = 0L
	private var episodeInteractMs = 0L
	private var episodeWasInterrupted: Boolean = false
	private var showStillWatching: Boolean = false

	// Preferences

	private val inAppEnabled get() = userPreferences[UserPreferences.screensaverInAppEnabled]
	private val timeout get() = userPreferences[UserPreferences.screensaverInAppTimeout].milliseconds

	// State

	private val _screensaverVisible = MutableStateFlow(false)
	val visible get() = _screensaverVisible.asStateFlow()

	private val _keepScreenOn = MutableStateFlow(inAppEnabled)
	val keepScreenOn get() = _keepScreenOn.asStateFlow()

	var activityPaused: Boolean = false
		set(value) {
			field = value
			notifyInteraction(canCancel = true, userInitiated = false)
		}

	init {
		notifyInteraction(canCancel = true, userInitiated = false)
	}

	fun getShowStillWatching(): Boolean {
		return showStillWatching
	}

	fun notifyStartSession(item: BaseItemDto, items: List<BaseItemDto>) {
		// No need to track when only watching 1 episode
		if (itemIsEpisode(item) && items.size > 1) {
			resetSession()
			episodeWasInterrupted = false
			showStillWatching = false
		}
	}

	fun onEpisodeWatched() {
		if (!episodeWasInterrupted) episodeCount++
		calculateWatchTime()
		episodeWasInterrupted = false
		checkStillWatchingStatus()
	}

	fun notifyStart(item: BaseItemDto) {
		if (itemIsEpisode(item)) {
			isWatchingEpisodes = true
		}
	}

	private fun checkStillWatchingStatus() {
		val presetName = userPreferences[UserPreferences.stillWatchingBehavior].toString().uppercase()
		val preset = runCatching { StillWatchingPresetConfigs.valueOf(presetName) }.getOrDefault(StillWatchingPresetConfigs.DISABLED)

		val stillWatchingSetting = StillWatchingStates.getSetting(preset)
		val episodeRequirementMet = episodeCount == stillWatchingSetting.episodeCount
		val watchTimeRequirementMet = watchTime >= stillWatchingSetting.minDuration.inWholeMilliseconds

		if (episodeRequirementMet || watchTimeRequirementMet) {
			showStillWatching = true
		}
	}

	fun notifyInteraction(canCancel: Boolean, userInitiated: Boolean) {
		// Cancel pending screensaver timer (if any)
		timer?.cancel()

		// If watching episodes, reset episode count and watch time
		if (isWatchingEpisodes && userInitiated) {
			resetSession()

			val playbackController: PlaybackController = playbackControllerContainer.playbackController!!

			episodeInteractMs = playbackController.currentPosition
			episodeWasInterrupted = true
		}

		// Hide screensaver when interacted with allowed cancellation or when disabled
		if (_screensaverVisible.value && (canCancel || !inAppEnabled || activityPaused)) {
			_screensaverVisible.value = false
		}

		// Create new timer to show screensaver when enabled
		if (inAppEnabled && !activityPaused && locks == 0) {
			timer = viewModelScope.launch {
				delay(timeout)
				_screensaverVisible.value = true
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
			notifyInteraction(canCancel = true, userInitiated = false)
			active = true
		}

		fun cancel() {
			if (!active) return
			locks--
			lifecycle.removeObserver(this)
			notifyInteraction(canCancel = false, userInitiated = false)
			active = false
		}
	}

	private fun itemIsEpisode(item: BaseItemDto? = null): Boolean {
		if (item != null) {
			return item.type == BaseItemKind.EPISODE
		}

		val playbackController = playbackControllerContainer.playbackController

		return playbackController?.currentlyPlayingItem?.type == BaseItemKind.EPISODE
	}

	private fun resetSession() {
		watchTime = 0L
		episodeCount = 0
		episodeInteractMs = 0L
	}

	private fun calculateWatchTime() {
		val duration = playbackControllerContainer.playbackController!!.duration
		val durationWatchedUninterrupted = if (episodeWasInterrupted) duration - episodeInteractMs else duration
		watchTime += durationWatchedUninterrupted
	}
}
