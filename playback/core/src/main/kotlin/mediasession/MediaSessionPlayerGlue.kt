package org.jellyfin.playback.core.mediasession

import androidx.media.AudioAttributesCompat
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jellyfin.playback.core.model.PlayState

// The base class of PlayerResult is marked as restricted causing a
// false-positive in the Android linter
@Suppress("RestrictedApi")
internal class MediaSessionPlayerGlue(
	private val scope: CoroutineScope,
	private val state: org.jellyfin.playback.core.PlayerState,
) : SessionPlayer() {
	private val callbackMutex = Mutex()
	fun notifyCallbacks(body: PlayerCallback.() -> Unit) {
		for (pair in callbacks) {
			val executor = pair.second!!
			val callback = pair.first!!

			scope.launch {
				callbackMutex.withLock {
					executor.execute {
						callback.body()
					}
				}
			}
		}
	}

	private var _currentMediaItem: MediaItem? = null

	fun setCurrentMediaItem(item: MediaItem?) {
		_currentMediaItem = item
	}

	override fun getCurrentMediaItem(): MediaItem? = _currentMediaItem

	override fun getDuration(): Long = runBlocking(Dispatchers.Main) {
		state.positionInfo.duration.inWholeMilliseconds
	}

	override fun setMediaItem(item: MediaItem) = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun updatePlaylistMetadata(metadata: MediaMetadata?) = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun setPlaybackSpeed(playbackSpeed: Float) = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun getPlaylist(): MutableList<MediaItem> = listOfNotNull(currentMediaItem).toMutableList()

	override fun getCurrentPosition(): Long = runBlocking(Dispatchers.Main) {
		state.positionInfo.active.inWholeMilliseconds
	}

	override fun play() = scope.future {
		state.unpause()
		PlayerResult(PlayerResult.RESULT_SUCCESS, currentMediaItem)
	}

	override fun skipToPreviousPlaylistItem() = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun getShuffleMode(): Int = SHUFFLE_MODE_NONE

	override fun getRepeatMode(): Int = REPEAT_MODE_NONE

	override fun getPlayerState(): Int = when (state.playState.value) {
		PlayState.STOPPED -> PLAYER_STATE_IDLE
		PlayState.PLAYING -> PLAYER_STATE_PLAYING
		PlayState.PAUSED -> PLAYER_STATE_PAUSED
		PlayState.ERROR -> PLAYER_STATE_ERROR
	}

	override fun setPlaylist(list: MutableList<MediaItem>, metadata: MediaMetadata?) = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun getPlaybackSpeed(): Float = state.speed.value

	override fun setShuffleMode(shuffleMode: Int) = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun skipToNextPlaylistItem() = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun getBufferedPosition(): Long = state.positionInfo.buffer.inWholeMilliseconds

	override fun replacePlaylistItem(index: Int, item: MediaItem) = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun getNextMediaItemIndex(): Int = 1

	override fun addPlaylistItem(index: Int, item: MediaItem) = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun seekTo(position: Long) = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun getBufferingState(): Int = BUFFERING_STATE_BUFFERING_AND_PLAYABLE

	override fun removePlaylistItem(index: Int) = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun setRepeatMode(repeatMode: Int) = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun skipToPlaylistItem(index: Int) = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun prepare() = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }


	override fun pause() = scope.future {
		state.pause()
		PlayerResult(PlayerResult.RESULT_SUCCESS, currentMediaItem)
	}

	override fun getPlaylistMetadata(): MediaMetadata? = null

	override fun getPreviousMediaItemIndex(): Int = INVALID_ITEM_INDEX

	override fun setAudioAttributes(attributes: AudioAttributesCompat) = scope.future { PlayerResult(PlayerResult.RESULT_ERROR_NOT_SUPPORTED, null) }

	override fun getAudioAttributes(): AudioAttributesCompat? = AudioAttributesCompat.Builder().build()

	override fun getCurrentMediaItemIndex(): Int = 1
}
