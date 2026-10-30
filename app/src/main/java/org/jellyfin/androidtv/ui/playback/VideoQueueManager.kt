package org.jellyfin.androidtv.ui.playback

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaStream
import java.util.UUID

class VideoQueueManager {
	private var _currentVideoQueue: List<BaseItemDto> = emptyList()
	private var _currentMediaPosition = -1
	private var _lastPlayedWorkId: UUID? = null
	private var _lastPlayedAudioStream: MediaStream? = null
	private var _lastPlayedSubtitleStream: MediaStream? = null

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

	fun setLastPlayedStreams(workId: UUID, audio: MediaStream?, subtitle: MediaStream?) {
		_lastPlayedWorkId = workId
		_lastPlayedAudioStream = audio
		_lastPlayedSubtitleStream = subtitle
	}

	fun getLastPlayedWorkId(): UUID? = _lastPlayedWorkId

	fun getLastPlayedAudioStream(): MediaStream? = _lastPlayedAudioStream

	fun getLastPlayedSubtitleStream(): MediaStream? = _lastPlayedSubtitleStream

	fun clearVideoQueue() {
		_currentVideoQueue = emptyList()
		_currentMediaPosition = -1
		_lastPlayedWorkId = null
		_lastPlayedAudioStream = null
		_lastPlayedSubtitleStream = null
	}
}
