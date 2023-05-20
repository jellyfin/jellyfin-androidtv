package org.jellyfin.playback.core.queue

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.item.QueueEntry
import timber.log.Timber
import kotlin.math.min
import kotlin.random.Random

class QueueService(
	private val setCurrentEntry: (QueueEntry?) -> Unit,
) : PlayerService() {
	private companion object {
		private const val POSITION_NONE = -1
	}

	private var currentQueueIndicesPlayed = mutableListOf<Int>()
	private var currentQueueNextIndices = mutableListOf<Int>()

	override suspend fun onInitialize() {
		// Reset calculated next-up indices when playback order changes
		state.playbackOrder.onEach {
			currentQueueNextIndices.clear()
		}.launchIn(coroutineScope)

		// Update current queue entry when queue changes
		state.queue.onEach { queue ->
			Timber.d("Queue changed, setting index to 0")

			currentQueueIndicesPlayed.clear()
			currentQueueNextIndices.clear()

			when (state.playbackOrder.value) {
				PlaybackOrder.DEFAULT -> setIndex(0)
				PlaybackOrder.RANDOM,
				PlaybackOrder.SHUFFLE -> setIndex((0 until queue.size).random())
			}
		}.launchIn(coroutineScope)

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

	var currentItemPosition = POSITION_NONE
		private set

	private fun getNextIndices(amount: Int, usePlaybackOrder: Boolean = true): List<Int> {
		val order = if (usePlaybackOrder) state.playbackOrder.value else PlaybackOrder.DEFAULT
		val queue = state.queue.value

		return when (order) {
			PlaybackOrder.DEFAULT -> {
				// No need to use currentQueueNextIndices because we can efficiently calculate the next items
				val remainingItemsSize = queue.size - currentItemPosition - 1
				if (remainingItemsSize <= 0) emptyList()
				else Array(min(amount, remainingItemsSize)) { i -> currentItemPosition + i + 1 }.toList()
			}

			PlaybackOrder.RANDOM -> Array(amount) { i ->
				if (i <= currentQueueNextIndices.lastIndex) {
					currentQueueNextIndices[i]
				} else {
					val index = Random.nextInt(queue.size)
					currentQueueNextIndices.add(index)
					index
				}
			}.toList()

			PlaybackOrder.SHUFFLE -> {
				val remainingItemsSize = queue.size - currentQueueIndicesPlayed.size
				if (remainingItemsSize <= 0) {
					emptyList()
				} else {
					val remainingIndices = (0..queue.size).filterNot {
						it in currentQueueIndicesPlayed || it in currentQueueNextIndices
					}.shuffled()

					Array(min(amount, remainingItemsSize)) { i ->
						if (i <= currentQueueNextIndices.lastIndex) {
							currentQueueNextIndices[i]
						} else {
							val index = remainingIndices[i - currentQueueNextIndices.size]
							currentQueueNextIndices.add(index)
							index
						}
					}.toList()
				}
			}
		}
	}

	// Jumping

	suspend fun previous(): QueueEntry? = currentQueueIndicesPlayed.removeLastOrNull()?.let {
		setIndex(it)
	}

	suspend fun next(usePlaybackOrder: Boolean = true): QueueEntry? {
		val index = getNextIndices(1, usePlaybackOrder).firstOrNull() ?: return null
		if (usePlaybackOrder) currentQueueNextIndices.removeFirstOrNull()

		return setIndex(index, true)
	}

	suspend fun setIndex(index: Int, saveHistory: Boolean = false): QueueEntry? {
		if (index < 0) return null

		// Save previous index
		if (saveHistory && currentItemPosition != POSITION_NONE) {
			currentQueueIndicesPlayed.add(currentItemPosition)
		}

		// Set new index
		val currentEntry = state.queue.value.getItem(index)
		currentItemPosition = if (currentEntry == null) POSITION_NONE else index
		setCurrentEntry(currentEntry)

		return currentEntry
	}

	// Peeking

	suspend fun peekPrevious(): QueueEntry? = currentQueueIndicesPlayed.lastOrNull()?.let {
		state.queue.value.getItem(it)
	}

	suspend fun peekNext(
		usePlaybackOrder: Boolean = true,
	): QueueEntry? = peekNext(1, usePlaybackOrder).firstOrNull()

	suspend fun peekNext(amount: Int, usePlaybackOrder: Boolean = true): Collection<QueueEntry> {
		val queue = state.queue.value
		return getNextIndices(amount, usePlaybackOrder).mapNotNull { index -> queue.getItem(index) }
	}
}

inline val PlaybackManager.queue get() = getService<QueueService>()
