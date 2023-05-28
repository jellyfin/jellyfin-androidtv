package org.jellyfin.playback.core.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.playback.core.PlayerState
import org.jellyfin.playback.core.backend.BackendService
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.queue.item.QueueEntry
import timber.log.Timber
import kotlin.math.min
import kotlin.random.Random

interface PlayerQueueState {
	companion object {
		const val INDEX_NONE = -1
	}

	val current: StateFlow<Queue>
	val entryIndex: StateFlow<Int>
	val entry: StateFlow<QueueEntry?>

	// Queue Management
	fun replaceQueue(queue: Queue)

	// Queue Seeking
	suspend fun previous(): QueueEntry?
	suspend fun next(usePlaybackOrder: Boolean = true): QueueEntry?
	suspend fun setIndex(index: Int, saveHistory: Boolean = false): QueueEntry?

	// Peeking
	suspend fun peekPrevious(): QueueEntry?
	suspend fun peekNext(usePlaybackOrder: Boolean = true): QueueEntry?
	suspend fun peekNext(amount: Int, usePlaybackOrder: Boolean = true): Collection<QueueEntry>
}

class DefaultPlayerQueueState(
	private val state: PlayerState,
	private val coroutineScope: CoroutineScope,
	backendService: BackendService,
) : PlayerQueueState {
	private val _current = MutableStateFlow<Queue>(EmptyQueue)
	override val current: StateFlow<Queue> get() = _current.asStateFlow()

	private val _entryIndex = MutableStateFlow(PlayerQueueState.INDEX_NONE)
	override val entryIndex: StateFlow<Int> get() = _entryIndex.asStateFlow()

	private val _entry = MutableStateFlow<QueueEntry?>(null)
	override val entry: StateFlow<QueueEntry?> get() = _entry.asStateFlow()

	private var currentQueueIndicesPlayed = mutableListOf<Int>()
	private var currentQueueNextIndices = mutableListOf<Int>()

	init {
		// Reset calculated next-up indices when playback order changes
		state.playbackOrder.onEach {
			currentQueueNextIndices.clear()
		}.launchIn(coroutineScope)

		backendService.addListener(object : PlayerBackendEventListener {
			override fun onPlayStateChange(state: PlayState) = Unit
			override fun onVideoSizeChange(width: Int, height: Int) = Unit

			override fun onMediaStreamEnd(mediaStream: MediaStream) {
				// TODO: Find position based on $mediaStream instead
				// TODO: This doesn't work as expected
				coroutineScope.launch { next() }
			}
		})
	}

	override fun replaceQueue(queue: Queue) {
		Timber.d("Queue changed, setting index to 0")

		_current.value = queue

		currentQueueIndicesPlayed.clear()
		currentQueueNextIndices.clear()

		coroutineScope.launch {
			when (state.playbackOrder.value) {
				PlaybackOrder.DEFAULT -> setIndex(0)
				PlaybackOrder.RANDOM,
				PlaybackOrder.SHUFFLE -> setIndex((0 until queue.size).random())
			}
		}
	}

	private fun getNextIndices(amount: Int, usePlaybackOrder: Boolean = true): List<Int> {
		val order = if (usePlaybackOrder) state.playbackOrder.value else PlaybackOrder.DEFAULT
		val queue = _current.value

		return when (order) {
			PlaybackOrder.DEFAULT -> {
				// No need to use currentQueueNextIndices because we can efficiently calculate the next items
				val remainingItemsSize = queue.size - _entryIndex.value - 1
				if (remainingItemsSize <= 0) emptyList()
				else Array(min(amount, remainingItemsSize)) { i -> _entryIndex.value + i + 1 }.toList()
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

	override suspend fun previous(): QueueEntry? = currentQueueIndicesPlayed.removeLastOrNull()?.let {
		setIndex(it)
	}

	override suspend fun next(usePlaybackOrder: Boolean): QueueEntry? {
		val index = getNextIndices(1, usePlaybackOrder).firstOrNull() ?: return null
		if (usePlaybackOrder) currentQueueNextIndices.removeFirstOrNull()

		return setIndex(index, true)
	}

	override suspend fun setIndex(index: Int, saveHistory: Boolean): QueueEntry? {
		if (index < 0) return null

		// Save previous index
		if (saveHistory && _entryIndex.value != PlayerQueueState.INDEX_NONE) {
			currentQueueIndicesPlayed.add(_entryIndex.value)
		}

		// Set new index
		val currentEntry = _current.value.getItem(index)
		_entryIndex.value = if (currentEntry == null) PlayerQueueState.INDEX_NONE else index
		_entry.value = currentEntry

		return currentEntry
	}

	// Peeking

	override suspend fun peekPrevious(): QueueEntry? = currentQueueIndicesPlayed.lastOrNull()?.let {
		_current.value.getItem(it)
	}

	override suspend fun peekNext(
		usePlaybackOrder: Boolean,
	): QueueEntry? = peekNext(1, usePlaybackOrder).firstOrNull()

	override suspend fun peekNext(amount: Int, usePlaybackOrder: Boolean): Collection<QueueEntry> {
		val queue = _current.value
		return getNextIndices(amount, usePlaybackOrder).mapNotNull { index -> queue.getItem(index) }
	}
}
