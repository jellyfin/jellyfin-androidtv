package org.jellyfin.androidtv.ui

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.androidtv.ui.playback.VideoManager
import java.util.logging.Logger

class WatchTrackerViewModel : ViewModel() {
	private lateinit var videoManager: VideoManager
	private var episodeCount = 0
	private var watchTime = 0L
	private var lastUpdateTime = 0L
	private var lastInteractionTime = System.currentTimeMillis()
	private var isPlaying: Boolean = false

	fun onEpisodeWatched() {
		Logger.getLogger(WatchTrackerViewModel::class.java.name).info("Watcher onEpisodeWatched")
		episodeCount++
	}

	fun onUserInteraction() {
		Logger.getLogger(WatchTrackerViewModel::class.java.name).info("Watcher onUserInteraction")
		resetWatchTime()
		lastInteractionTime = System.currentTimeMillis()
	}

	fun startWatchTime() {
		if (isPlaying) return
		Logger.getLogger(WatchTrackerViewModel::class.java.name).info("Start tracker")
		resetWatchTime()
		lastUpdateTime = System.currentTimeMillis()
		isPlaying = true
		viewModelScope.launch {
			while (isPlaying) {
				calculateWatchTime()
				checkPrompt()
				delay(5000)
			}
		}
	}

	fun stopWatchTime() {
		isPlaying = false
		viewModelScope.coroutineContext.cancelChildren()
	}

	fun setVideoManager(videoManager: VideoManager) {
		this.videoManager = videoManager
	}

	private fun checkPrompt() {
		if (episodeCount == 3 || watchTime >= 6 * 1000) {
			videoManager.pause()
			Logger.getLogger(WatchTrackerViewModel::class.java.name).info("Prompt user")

			val activity = videoManager.getActivity() as? FragmentActivity
			val fragmentManager = activity?.supportFragmentManager
			fragmentManager?.let {
				StillWatchingDialogFragment().show(it, "StillWatchingDialog")
			}
		}
	}

	fun onPromptDismissed() {
	}

	private fun resetWatchTime() {
		watchTime = 0L
		episodeCount = 0
		isPlaying = true
	}

	private fun calculateWatchTime() {
		val currentTime = System.currentTimeMillis()
		watchTime += currentTime - lastUpdateTime
		lastUpdateTime = currentTime
		Logger.getLogger(WatchTrackerViewModel::class.java.name).info("Watcher watchTime: $watchTime")
	}
}
