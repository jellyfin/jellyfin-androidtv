package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.sdk.model.api.BaseItemDto

class VideoQueueManager(
	private val userRepository: UserRepository,
) {
	private var _currentVideoQueue: List<BaseItemDto> = emptyList()
	private var _currentMediaPosition = -1
	private var _lastPlayedAudioLanguageIsoCode: String? = null
	private var _lastPlayedSubtitleLanguageIsoCode: String? = null

	fun setCurrentVideoQueue(items: List<BaseItemDto>?) {
		if (items.isNullOrEmpty()) return clearVideoQueue()

		_currentVideoQueue = items.toMutableList()
		_currentMediaPosition = 0
	}

	fun getCurrentVideoQueue(): List<BaseItemDto> = _currentVideoQueue

	fun setCurrentMediaPosition(currentMediaPosition: Int) {
		if (currentMediaPosition !in 0.._currentVideoQueue.size) return

		_currentMediaPosition = currentMediaPosition
	}

	fun getCurrentMediaPosition() = _currentMediaPosition

	fun getLastPlayedAudioLanguageIsoCode(): String? {
		return _lastPlayedAudioLanguageIsoCode.takeIf {
			userRepository.currentUser.value?.configuration?.rememberAudioSelections == true
		}
	}

	@JvmOverloads
	fun setLastPlayedAudioLanguageIsoCode(isoCode: String, selectedByUser: Boolean = true) {
		// Automatic fallback is not a user preference. Keep the last explicit selection.
		if (selectedByUser) _lastPlayedAudioLanguageIsoCode = isoCode
	}

	fun getLastPlayedSubtitleLanguageIsoCode(): String? {
		return _lastPlayedSubtitleLanguageIsoCode
	}

	fun setLastPlayedSubtitleLanguageIsoCode(isoCode: String?) {
		_lastPlayedSubtitleLanguageIsoCode = isoCode
	}

	fun clearVideoQueue() {
		_currentVideoQueue = emptyList()
		_currentMediaPosition = -1
		_lastPlayedAudioLanguageIsoCode = null
		_lastPlayedSubtitleLanguageIsoCode = null
	}
}
