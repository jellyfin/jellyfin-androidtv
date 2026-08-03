package org.jellyfin.androidtv.ui.playback

import org.jellyfin.sdk.model.api.BaseItemDto

class VideoQueueManager {
	private var _currentVideoQueue: List<BaseItemDto> = emptyList()
	private var _currentMediaPosition = -1
	private var _lastPlayedAudioDefaultState: Boolean = false
	private var _lastPlayedAudioCodec: String? = null
	private var _lastPlayedAudioHearingImpairedState: Boolean = false
	private var _lastPlayedAudioLanguageIsoCode: String? = null
	private var _lastPlayedSubtitleCodec: String? = null
	private var _lastPlayedSubtitleDefaultState: Boolean = false
	private var _lastPlayedSubtitleForcedState: Boolean = false
	private var _lastPlayedSubtitleHearingImpairedState: Boolean = false
	private var _lastPlayedSubtitleLanguageIsoCode: String? = null
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

	fun getLastPlayedAudioCodec(): String? {
		return _lastPlayedAudioCodec
	}

	fun setLastPlayedAudioCodec(codec: String) {
		_lastPlayedAudioCodec = codec
	}
	fun getLastPlayedAudioDefaultState (): Boolean {
		return _lastPlayedAudioDefaultState
	}

	fun setLastPlayedAudioDefaultState(state: Boolean) {
		_lastPlayedAudioDefaultState = state
	}

	fun getLastPlayedAudioHearingImpairedState(): Boolean {
		return _lastPlayedAudioHearingImpairedState
	}

	fun setLastPlayedAudioHearingImpairedState(state: Boolean) {
		_lastPlayedAudioHearingImpairedState = state
	}

	fun getLastPlayedAudioLanguageIsoCode(): String? {
		return _lastPlayedAudioLanguageIsoCode
	}

	fun setLastPlayedAudioLanguageIsoCode(isoCode: String) {
		_lastPlayedAudioLanguageIsoCode = isoCode
	}

	fun getLastPlayedSubtitleCodec(): String? {
		return _lastPlayedSubtitleCodec
	}

	fun setLastPlayedSubtitleCodec(codecTag: String?) {
		_lastPlayedSubtitleCodec = codecTag
	}

	fun getLastPlayedSubtitleDefaultState(): Boolean {
		return _lastPlayedSubtitleDefaultState
	}

	fun setLastPlayedSubtitleDefaultState(state: Boolean) {
		_lastPlayedSubtitleDefaultState = state
	}

	fun getLastPlayedSubtitleForcedState(): Boolean {
		return _lastPlayedSubtitleForcedState
	}

	fun setLastPlayedSubtitleForcedState(state: Boolean) {
		_lastPlayedSubtitleForcedState = state
	}

	fun getLastPlayedSubtitleHearingImpairedState(): Boolean {
		return _lastPlayedSubtitleHearingImpairedState
	}

	fun setLastPlayedSubtitleHearingImpairedState(state: Boolean) {
		_lastPlayedSubtitleHearingImpairedState = state
	}

	fun getLastPlayedSubtitleLanguageIsoCode(): String? {
		return _lastPlayedSubtitleLanguageIsoCode
	}

	fun setLastPlayedSubtitleLanguageIsoCode(isoCode: String?) {
		_lastPlayedSubtitleLanguageIsoCode = isoCode
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
		_lastPlayedAudioCodec = null
		_lastPlayedAudioDefaultState = false
		_lastPlayedAudioHearingImpairedState = false
		_lastPlayedAudioLanguageIsoCode = null
		_lastPlayedSubtitleCodec = null
		_lastPlayedSubtitleDefaultState = false
		_lastPlayedSubtitleForcedState = false
		_lastPlayedSubtitleHearingImpairedState = false
		_lastPlayedSubtitleLanguageIsoCode = null
		_lastPlayedSubtitleTitle = null
	}
}
