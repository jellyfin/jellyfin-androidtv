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
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.model.RepeatMode
import org.jellyfin.playback.core.queue.item.QueueEntry
import org.jellyfin.playback.core.queue.order.DefaultOrderIndexProvider
import org.jellyfin.playback.core.queue.order.OrderIndexProvider
import org.jellyfin.playback.core.queue.order.RandomOrderIndexProvider
import org.jellyfin.playback.core.queue.order.ShuffleOrderIndexProvider
import timber.log.Timber

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
	suspend fun next(usePlaybackOrder: Boolean = true, useRepeatMode: Boolean = false): QueueEntry?
	suspend fun setIndex(index: Int, saveHistory: Boolean = false): QueueEntry?

	// Peeking
	suspend fun peekPrevious(): QueueEntry?
	suspend fun peekNext(usePlaybackOrder: Boolean = true, useRepeatMode: Boolean = false): QueueEntry?
	suspend fun peekNext(
		amount: Int,
		usePlaybackOrder: Boolean = true,
		useRepeatMode: Boolean = false
	): Collection<QueueEntry>
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

	private var defaultOrderIndexProvider = DefaultOrderIndexProvider()
	private var orderIndexProvider: OrderIndexProvider = defaultOrderIndexProvider
	private var currentQueueIndicesPlayed = mutableListOf<Int>()

	init {
		// Reset calculated next-up indices when playback order changes
		state.playbackOrder.onEach { playbackOrder ->
			orderIndexProvider = when (playbackOrder) {
				PlaybackOrder.DEFAULT -> defaultOrderIndexProvider
				PlaybackOrder.RANDOM -> RandomOrderIndexProvider()
				PlaybackOrder.SHUFFLE -> ShuffleOrderIndexProvider()
			}
		}.launchIn(coroutineScope)

		backendService.addListener(object : PlayerBackendEventListener {
			override fun onPlayStateChange(state: PlayState) = Unit
			override fun onVideoSizeChange(width: Int, height: Int) = Unit

			override fun onMediaStreamEnd(mediaStream: PlayableMediaStream) {
				// TODO: Find position based on $mediaStream instead
				// TODO: This doesn't work as expected
				coroutineScope.launch { next(usePlaybackOrder = true, useRepeatMode = true) }
			}
		})
	}

	override fun replaceQueue(queue: Queue) {
		Timber.d("Queue changed, setting index to 0")

		coroutineScope.launch {
			_current.value = queue
			orderIndexProvider.reset()
			if (orderIndexProvider != defaultOrderIndexProvider) defaultOrderIndexProvider.reset()

			currentQueueIndicesPlayed.clear()

			setIndex(0)
		}
	}

	private fun getNextIndices(amount: Int, usePlaybackOrder: Boolean, useRepeatMode: Boolean): Collection<Int> {
		val provider = if (usePlaybackOrder) orderIndexProvider else defaultOrderIndexProvider
		val repeatMode = if (useRepeatMode) state.repeatMode.value else RepeatMode.NONE

		return when (repeatMode) {
			RepeatMode.NONE -> provider.provideIndices(amount, _current.value, currentQueueIndicesPlayed, entryIndex.value)

			RepeatMode.REPEAT_ENTRY_ONCE -> buildList {
				add(entryIndex.value)
				addAll(provider.provideIndices(amount, _current.value, currentQueueIndicesPlayed, entryIndex.value))
			}.take(amount)

			RepeatMode.REPEAT_ENTRY_INFINITE -> List(amount) { entryIndex.value }
		}
	}

	// Jumping

	override suspend fun previous(): QueueEntry? = currentQueueIndicesPlayed.removeLastOrNull()?.let {
		setIndex(it)
	}

	override suspend fun next(usePlaybackOrder: Boolean, useRepeatMode: Boolean): QueueEntry? {
		val index = getNextIndices(1, usePlaybackOrder, useRepeatMode).firstOrNull() ?: return null
		if (usePlaybackOrder) {
			// Automatically set repeat mode back to none when using the ONCE option
			if (state.repeatMode.value == RepeatMode.REPEAT_ENTRY_ONCE && index == this._entryIndex.value) {
				state.setRepeatMode(RepeatMode.NONE)
			} else if (state.repeatMode.value == RepeatMode.NONE) {
				orderIndexProvider.useNextIndex()
			}
		}

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
		useRepeatMode: Boolean,
	): QueueEntry? = peekNext(1, usePlaybackOrder, useRepeatMode).firstOrNull()

	override suspend fun peekNext(
		amount: Int,
		usePlaybackOrder: Boolean,
		useRepeatMode: Boolean,
	): Collection<QueueEntry> {
		val queue = _current.value
		return getNextIndices(amount, usePlaybackOrder, useRepeatMode)
			.mapNotNull { index -> queue.getItem(index) }
	}
}
