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
	fun clearAudioQueue(releasePlayer: Boolean)
	fun addToAudioQueue(items: List<BaseItemDto>)
	fun removeFromAudioQueue(ndx: Int)
	val isPlayingAudio: Boolean
	fun playNow(context: Context, items: List<BaseItemDto>, position: Int, shuffle: Boolean)
	fun playNow(context: Context, items: List<BaseItemDto>, shuffle: Boolean)
	fun playNow(context: Context, item: BaseItemDto)
	fun playFrom(ndx: Int): Boolean
	fun shuffleAudioQueue()
	val nextAudioItem: BaseItemDto?
	val prevAudioItem: BaseItemDto?
	fun hasNextAudioItem(): Boolean
	fun hasPrevAudioItem(): Boolean
	fun nextAudioItem(): Int
	fun prevAudioItem(): Int
	fun stopAudio(releasePlayer: Boolean)
	fun pauseAudio()
	fun playPauseAudio()
	fun resumeAudio()
	fun fastForward()
	fun rewind()
}
