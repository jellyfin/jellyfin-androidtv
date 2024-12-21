package org.jellyfin.playback.core.queue

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.model.RepeatMode
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.order.DefaultOrderIndexProvider
import org.jellyfin.playback.core.queue.order.OrderIndexProvider
import org.jellyfin.playback.core.queue.order.RandomOrderIndexProvider
import org.jellyfin.playback.core.queue.order.ShuffleOrderIndexProvider
import org.jellyfin.playback.core.queue.supplier.QueueSupplier
import kotlin.math.max

class QueueService internal constructor() : PlayerService(), Queue {
	private val suppliers = mutableListOf<QueueSupplier>()
	private var currentSupplierIndex = 0
	private var currentSupplierItemIndex = 0
	private val fetchedItems: MutableList<QueueEntry> = mutableListOf()

	private var defaultOrderIndexProvider = DefaultOrderIndexProvider()
	private var orderIndexProvider: OrderIndexProvider = defaultOrderIndexProvider
	private var currentQueueIndicesPlayed = mutableListOf<Int>()

	override val estimatedSize get() = max(fetchedItems.size, suppliers.sumOf { it.size })

	private val _entryIndex = MutableStateFlow(Queue.INDEX_NONE)
	override val entryIndex: StateFlow<Int> get() = _entryIndex.asStateFlow()

	private val _entry = MutableStateFlow<QueueEntry?>(null)
	override val entry: StateFlow<QueueEntry?> get() = _entry.asStateFlow()

	override suspend fun onInitialize() {
		// Reset calculated next-up indices when playback order changes
		state.playbackOrder.onEach { playbackOrder ->
			orderIndexProvider = when (playbackOrder) {
				PlaybackOrder.DEFAULT -> defaultOrderIndexProvider
				PlaybackOrder.RANDOM -> RandomOrderIndexProvider()
				PlaybackOrder.SHUFFLE -> ShuffleOrderIndexProvider()
			}
		}.launchIn(coroutineScope)

		// Automatically advance when current stream ends
		manager.backendService.addListener(object : PlayerBackendEventListener {
			override fun onPlayStateChange(state: PlayState) = Unit
			override fun onVideoSizeChange(width: Int, height: Int) = Unit
			override fun onMediaStreamEnd(mediaStream: PlayableMediaStream) {
				coroutineScope.launch {
					val nextItem = next(usePlaybackOrder = true, useRepeatMode = true)
					if (nextItem == null && _entryIndex.value != Queue.INDEX_NONE) setIndex(Queue.INDEX_NONE, true)
				}
			}
		})
	}

	// Entry management

	override fun addSupplier(supplier: QueueSupplier) {
		suppliers.add(supplier)

		if (_entryIndex.value == Queue.INDEX_NONE) {
			coroutineScope.launch { setIndex(0) }
		}
	}

	private suspend fun getOrSupplyItem(index: Int): QueueEntry? {
		// Fetch additional items from suppliers until we reach the desired index
		while (index >= fetchedItems.size) {
			// No more suppliers to try
			if (currentSupplierIndex >= suppliers.size) break

			val supplier = suppliers[currentSupplierIndex]
			val nextItem = supplier.getItem(currentSupplierItemIndex)

			if (nextItem != null) {
				// Add item to cache and icnrease item index
				fetchedItems.add(nextItem)
				currentSupplierItemIndex++
			} else {
				// Move to the next supplier if current one is exhausted
				currentSupplierIndex++
				currentSupplierItemIndex = 0
			}
		}

		// Return item or null if not found
		return if (index >= 0 && index < fetchedItems.size) fetchedItems[index]
		else null
	}

	override fun clear() {
		suppliers.clear()
		currentSupplierIndex = 0
		currentSupplierItemIndex = 0
		fetchedItems.clear()
		_entry.value = null
		_entryIndex.value = Queue.INDEX_NONE
		currentQueueIndicesPlayed.clear()
	}

	// Preloading

	private fun getNextIndices(amount: Int, usePlaybackOrder: Boolean, useRepeatMode: Boolean): Collection<Int> {
		val provider = if (usePlaybackOrder) orderIndexProvider else defaultOrderIndexProvider
		val repeatMode = if (useRepeatMode) state.repeatMode.value else RepeatMode.NONE

		return when (repeatMode) {
			RepeatMode.NONE -> provider.provideIndices(amount, estimatedSize, currentQueueIndicesPlayed, _entryIndex.value)

			RepeatMode.REPEAT_ENTRY_ONCE -> buildList(amount) {
				add(_entryIndex.value)
				addAll(provider.provideIndices(amount - 1, estimatedSize, currentQueueIndicesPlayed, _entryIndex.value))
			}.take(amount)

			RepeatMode.REPEAT_ENTRY_INFINITE -> List(amount) { _entryIndex.value }
		}
	}

	// Jumping

	override suspend fun previous(): QueueEntry? = currentQueueIndicesPlayed.removeLastOrNull()?.let {
		setIndex(it)
	}

	override suspend fun next(usePlaybackOrder: Boolean, useRepeatMode: Boolean): QueueEntry? {
		val index = getNextIndices(1, usePlaybackOrder, useRepeatMode).firstOrNull() ?: return null

		val provider = if (usePlaybackOrder) orderIndexProvider else defaultOrderIndexProvider
		val repeatMode = if (useRepeatMode) state.repeatMode.value else RepeatMode.NONE

		// Automatically set repeat mode back to none when using the ONCE option
		if (repeatMode == RepeatMode.REPEAT_ENTRY_ONCE && index == this._entryIndex.value) {
			state.setRepeatMode(RepeatMode.NONE)
		} else if (repeatMode == RepeatMode.NONE) {
			provider.useNextIndex()
		}

		return setIndex(index, true)
	}

	override suspend fun setIndex(index: Int, saveHistory: Boolean): QueueEntry? {
		if (index < 0 && index != Queue.INDEX_NONE) return null

		// Save previous index
		if (saveHistory && _entryIndex.value != Queue.INDEX_NONE) {
			currentQueueIndicesPlayed.add(_entryIndex.value)
		}

		// Set new index
		val currentEntry = getOrSupplyItem(index)
		_entryIndex.value = if (currentEntry == null) Queue.INDEX_NONE else index
		_entry.value = currentEntry

		return currentEntry
	}

	// Peeking

	override suspend fun peekPrevious(): QueueEntry? = currentQueueIndicesPlayed.lastOrNull()?.let {
		getOrSupplyItem(it)
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
		return getNextIndices(amount, usePlaybackOrder, useRepeatMode)
			.mapNotNull { index -> getOrSupplyItem(index) }
	}
}

val PlaybackManager.queue: Queue get() = requireNotNull(getService<QueueService>())
