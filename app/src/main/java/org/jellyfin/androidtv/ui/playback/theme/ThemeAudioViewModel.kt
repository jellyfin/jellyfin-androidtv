package org.jellyfin.androidtv.ui.playback.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.repository.ThemeAudioRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.AudioEventListener
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
import java.util.UUID

class ThemeAudioViewModel(
    private val repository: ThemeAudioRepository,
    private val player: ThemeAudioPlayer,
    private val userPreferences: UserPreferences,
    private val mediaManager: MediaManager
) : ViewModel(), AudioEventListener {

	private var currentJob: Job? = null
	private var lastFocusedItemId: UUID? = null
	private var currentPlayingUrl: String? = null

	init {
		mediaManager.addAudioEventListener(this)
	}

	fun onItemFocused(itemId: UUID?) {

		val isThemeAudioEnabled = userPreferences.get(UserPreferences.playThemeAudio)
		if (itemId == lastFocusedItemId) return
		if (mediaManager.isPlayingAudio) return

		lastFocusedItemId = itemId

		currentJob?.cancel()

		if (itemId == null || !isThemeAudioEnabled) {
			stopThemeAudio()
			return
		}

		currentJob = viewModelScope.launch {
			delay(800) // Debounce

			val url = repository.getThemeAudioUrl(itemId)

			if (url == currentPlayingUrl && currentPlayingUrl != null) {
				return@launch
			}

			player.stop()
			currentPlayingUrl = url

			if (url != null && !mediaManager.isPlayingAudio) {
				Timber.v("ThemeAudioViewModel: theme media started")
				player.play(url)
			}
		}
	}

	private fun stopThemeAudio() {
		player.stop()
		currentPlayingUrl = null
	}

	fun onItemUnfocused() {
		currentJob?.cancel()
		stopThemeAudio()
		lastFocusedItemId = null
	}

	// Stop theme if media is starting
	override fun onPlaybackStateChange(newState: PlaybackController.PlaybackState, currentItem: BaseItemDto?) {
		if (newState == PlaybackController.PlaybackState.PLAYING) {
			onItemUnfocused()
		}
	}

	override fun onCleared() {
		super.onCleared()
		mediaManager.removeAudioEventListener(this)
		player.release()
	}
}
