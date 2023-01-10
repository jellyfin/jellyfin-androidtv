package org.jellyfin.playback.core.queue

import kotlinx.coroutines.launch
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.item.QueueEntry

class QueueService(
	private val setCurrentEntry: (QueueEntry?) -> Unit,
) : PlayerService() {
	private companion object {
		private const val POSITION_NONE = -1
	}

	override suspend fun onInitialize() {
		coroutineScope.launch {
			// Update current queue entry when queue changes
			state.queue.collect { setIndex(0) }
		}
	}

	private var currentItemPosition = POSITION_NONE

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
