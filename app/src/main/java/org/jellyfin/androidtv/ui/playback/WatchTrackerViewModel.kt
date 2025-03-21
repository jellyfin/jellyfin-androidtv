package org.jellyfin.androidtv.ui.playback

import androidx.lifecycle.ViewModel
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.stillwatching.StillWatchingFragment
import org.jellyfin.androidtv.ui.playback.stillwatching.StillWatchingStates
import org.jellyfin.androidtv.util.TimeUtils.MILLIS_PER_MIN
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber


class WatchTrackerViewModel : ViewModel(), KoinComponent {
	private lateinit var videoManager: VideoManager
	private var episodeCount = 0
	private var watchTime = 0L
	private var lastUpdateTime = 0L
	private var itemWasInterrupted: Boolean = false

	private val userPreferences by inject<UserPreferences>()
	private val playbackControllerContainer by inject<PlaybackControllerContainer>()

	fun notifyStart(item: BaseItemDto, items: List<BaseItemDto>) {
		// No need to track when only watching 1 episode
		if (itemIsEpisode(item) && items.size > 1) {
			Timber.i("Start tracker")
			resetWatchTime()
			updateLastUpdateTime()
			itemWasInterrupted = false
		}
	}

	fun setVideoManager(videoManager: VideoManager) {
		this.videoManager = videoManager
	}

	fun onEpisodeWatched() {
		Timber.i("Watcher onEpisodeWatched")
		if (!itemWasInterrupted) episodeCount++
		itemWasInterrupted = false
	}

	fun notifyInteraction() {
		if (itemIsEpisode()) {
			Timber.i("Watcher onUserInteraction")
			resetWatchTime()
			itemWasInterrupted = true
		}
	}

	fun notifyPlay() {
		if (itemIsEpisode()) {
			calculateWatchTime()
			checkPrompt()
		}
	}

	fun notifyProgress() {
		if (itemIsEpisode() && !itemWasInterrupted) {
			calculateWatchTime()
			checkPrompt()
		}
	}

	private fun itemIsEpisode(item: BaseItemDto? = null): Boolean {
		if (item != null) {
			return item.type == BaseItemKind.EPISODE
		}

		val playbackController = playbackControllerContainer.playbackController

		return playbackController?.currentlyPlayingItem?.type == BaseItemKind.EPISODE
	}

	private fun updateLastUpdateTime() {
		lastUpdateTime = System.currentTimeMillis()
	}

	private fun checkPrompt() {
		if (!::videoManager.isInitialized) return

		val stillWatchingSetting = StillWatchingStates.getSetting(userPreferences[UserPreferences.stillWatchingEnabled].toString())

		val currentEpisodeProgress = videoManager.currentPosition.toFloat() / videoManager.duration.toFloat()
		val minMinutesInMs = stillWatchingSetting.minMinutes.toLong() * MILLIS_PER_MIN

		// At episode count, your watch time is above min minute threshold, and you are at least 10% of the way through the next episode
		val episodeRequirementMet =
			episodeCount == stillWatchingSetting.episodeCount &&
			watchTime >= minMinutesInMs &&
			currentEpisodeProgress >= 0.1

		// Above min minute threshold and you have watched more episodes than is required
		val watchTimeRequirementMet = watchTime >= minMinutesInMs && episodeCount > stillWatchingSetting.episodeCount

		if (episodeRequirementMet || watchTimeRequirementMet) {
			videoManager.pause()
			Timber.i("Prompt user")

			val activity = videoManager.activity
			val fragmentManager = activity.supportFragmentManager

			fragmentManager.let {
				StillWatchingFragment(videoManager).show(it, "STILL_WATCHING")
			}
		}
	}

	private fun resetWatchTime() {
		watchTime = 0L
		episodeCount = 0
	}


	private fun calculateWatchTime() {
		val currentTime = System.currentTimeMillis()
		watchTime += currentTime - lastUpdateTime
		lastUpdateTime = currentTime
	}
}
