package org.jellyfin.androidtv.ui.playback

import org.jellyfin.sdk.model.api.BaseItemDto

class VideoQueueManager {
	private var _currentVideoQueue: List<BaseItemDto> = emptyList()
	private var _currentMediaPosition = -1
	private var _lastPlayedAudioLanguageIsoCode: String? = null
	private var _lastPlayedAudioCodec: String? = null
	private var _lastPlayedSubtitleLanguageIsoCode: String? = null
	private var _lastPlayedSubtitleForcedState: Boolean = false
	private var _lastPlayedSubtitleCodec: String? = null
	private var _lastPlayedSubtitleTitle: String? = null

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
		return _lastPlayedAudioLanguageIsoCode
	}

	fun setLastPlayedAudioLanguageIsoCode(isoCode: String) {
		_lastPlayedAudioLanguageIsoCode = isoCode
	}

	fun getLastPlayedAudioCodec(): String? {
		return _lastPlayedAudioCodec
	}

	fun setLastPlayedAudioCodec(codec: String) {
		_lastPlayedAudioCodec = codec
	}

	fun getLastPlayedSubtitleLanguageIsoCode(): String? {
		return _lastPlayedSubtitleLanguageIsoCode
	}

	fun setLastPlayedSubtitleLanguageIsoCode(isoCode: String?) {
		_lastPlayedSubtitleLanguageIsoCode = isoCode
	}

	fun getLastPlayedSubtitleForcedState(): Boolean {
		return _lastPlayedSubtitleForcedState
	}

	fun setLastPlayedSubtitleForcedState(state: Boolean) {
		_lastPlayedSubtitleForcedState = state
	}

	fun getLastPlayedSubtitleCodec(): String? {
		return _lastPlayedSubtitleCodec
	}

	fun setLastPlayedSubtitleCodec(codecTag: String?) {
		_lastPlayedSubtitleCodec = codecTag
	}

	fun getLastPlayedSubtitleTitle(): String? {
		return _lastPlayedSubtitleTitle
	}

	fun setLastPlayedSubtitleTitle(title: String?) {
		_lastPlayedSubtitleTitle = title
	}

	fun clearVideoQueue() {
		_currentVideoQueue = emptyList()
		_currentMediaPosition = -1
		_lastPlayedAudioLanguageIsoCode = null
		_lastPlayedAudioCodec = null
		_lastPlayedSubtitleLanguageIsoCode = null
		_lastPlayedSubtitleForcedState = false
		_lastPlayedSubtitleCodec = null
		_lastPlayedSubtitleTitle = null
	}
}
