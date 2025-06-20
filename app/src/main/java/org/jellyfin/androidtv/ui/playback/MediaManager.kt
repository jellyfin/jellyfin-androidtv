package org.jellyfin.androidtv.ui.playback

import android.content.Context
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
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
	val currentAudioQueue: ItemRowAdapter?
	val managedAudioQueue: ItemRowAdapter?
	fun addAudioEventListener(listener: AudioEventListener)
	fun removeAudioEventListener(listener: AudioEventListener)
	fun queueAudioItem(item: BaseItemDto)
	fun clearAudioQueue()
	fun addToAudioQueue(items: List<BaseItemDto>)
	fun removeFromAudioQueue(item: BaseItemDto)
	val isPlayingAudio: Boolean
	fun playNow(context: Context, items: List<BaseItemDto>, position: Int, shuffle: Boolean)
	fun playFrom(item: BaseItemDto): Boolean
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
