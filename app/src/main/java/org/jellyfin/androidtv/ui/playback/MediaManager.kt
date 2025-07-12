package org.jellyfin.androidtv.ui.playback

import android.content.Context
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.sdk.model.api.BaseItemDto

interface MediaManager {
	fun hasAudioQueueItems(): Boolean
	val currentAudioQueueSize: Int
	val currentAudioQueuePosition: Int
	val currentAudioPosition: Long
	val currentAudioQueueDisplayPosition: String?
	val currentAudioQueueDisplaySize: String?
	val currentAudioItem: BaseItemDto?
	fun toggleRepeat(): Boolean
	val isRepeatMode: Boolean
	val isAudioPlayerInitialized: Boolean
	val isShuffleMode: Boolean
	fun addAudioEventListener(listener: AudioEventListener)
	fun removeAudioEventListener(listener: AudioEventListener)
	fun queueAudioItem(item: BaseItemDto)
	fun clearAudioQueue()
	fun addToAudioQueue(items: List<BaseItemDto>)
	fun removeFromAudioQueue(entry: QueueEntry)
	val isPlayingAudio: Boolean
	fun playNow(context: Context, items: List<BaseItemDto>, position: Int, shuffle: Boolean)
	fun playFrom(entry: QueueEntry): Boolean
	fun shuffleAudioQueue()
	fun hasNextAudioItem(): Boolean
	fun hasPrevAudioItem(): Boolean
	fun nextAudioItem(): Int
	fun prevAudioItem(): Int
	fun stopAudio(releasePlayer: Boolean)
	fun togglePlayPause()
	fun fastForward()
	fun rewind()
}
