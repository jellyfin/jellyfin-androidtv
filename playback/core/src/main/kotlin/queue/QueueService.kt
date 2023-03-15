package org.jellyfin.playback.core.queue

import kotlinx.coroutines.launch
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.item.QueueEntry
import timber.log.Timber

class QueueService(
	private val setCurrentEntry: (QueueEntry?) -> Unit,
) : PlayerService() {
	private companion object {
		private const val POSITION_NONE = -1
	}

	override suspend fun onInitialize() {
		coroutineScope.launch {
			// Update current queue entry when queue changes
			state.queue.collect {
				Timber.d("Queue changed, setting index to 0")
				setIndex(0)
			}

			manager.backendService.addListener(object : PlayerBackendEventListener {
				override fun onPlayStateChange(state: PlayState) = Unit
				override fun onVideoSizeChange(width: Int, height: Int) = Unit

				override fun onMediaStreamEnd(mediaStream: MediaStream) {
					// TODO: Find position based on $mediaStream instead
					// TODO: This doesn't work as expected
					coroutineScope.launch { next() }
				}
			})
		}
	}

	var currentItemPosition = POSITION_NONE
		private set

	// Jumping

	suspend fun previous(): QueueEntry? = setIndex(currentItemPosition - 1)
	suspend fun next(): QueueEntry? = setIndex(currentItemPosition + 1)

	suspend fun setIndex(index: Int): QueueEntry? {
		if (index < 0) return null

		val currentEntry = state.queue.value.getItem(index)
		currentItemPosition = if (currentEntry == null) POSITION_NONE else index
		setCurrentEntry(currentEntry)

		return currentEntry
	}

	// Peeking

	suspend fun peekPrevious(): QueueEntry? = state.queue.value.getItem(currentItemPosition + 1)
	suspend fun peekNext(): QueueEntry? = state.queue.value.getItem(currentItemPosition + 1)

	suspend fun peekNext(amount: Int): Collection<QueueEntry> = Array(amount) { i ->
		state.queue.value.getItem(currentItemPosition + i + 1)
	}.filterNotNull()
}

inline val PlaybackManager.queue get() = getService<QueueService>()
