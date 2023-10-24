package org.jellyfin.androidtv.ui.playback.rewrite

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.ui.itemhandling.AudioQueueItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.AudioEventListener
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.model.RepeatMode
import org.jellyfin.playback.core.queue.Queue
import org.jellyfin.playback.core.queue.item.QueueEntry
import org.jellyfin.playback.jellyfin.queue.item.BaseItemDtoUserQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import kotlin.math.max

@Suppress("TooManyFunctions")
class RewriteMediaManager(
	context: Context,
	api: ApiClient,
	private val navigationRepository: NavigationRepository,
	private val playbackManager: PlaybackManager,
) : MediaManager {
	private val queue = BaseItemQueue(api)

	override fun hasAudioQueueItems(): Boolean = currentAudioQueue.size() > 0 && currentAudioItem != null

	override val currentAudioQueueSize: Int
		get() = currentAudioQueue.size()

	override val currentAudioQueuePosition: Int
		get() = if ((playbackManager.state.queue.entryIndex.value) >= 0) 0 else -1

	override val currentAudioPosition: Long
		get() = playbackManager.state.positionInfo.active.inWholeMilliseconds

	override val currentAudioQueueDisplayPosition: String
		get() = (currentAudioQueuePosition + 1).toString()

	override val currentAudioQueueDisplaySize: String
		get() = ((playbackManager.state.queue.current.value as? BaseItemQueue)?.items?.size
			?: currentAudioQueue.size()).toString()

	override val currentAudioItem: BaseItemDto?
		get() = (playbackManager.state.queue.entry.value as? BaseItemDtoUserQueueEntry)?.baseItem

	override fun toggleRepeat(): Boolean {
		val newMode = when (playbackManager.state.repeatMode.value) {
			RepeatMode.NONE -> RepeatMode.REPEAT_ENTRY_INFINITE
			else -> RepeatMode.NONE
		}
		playbackManager.state.setRepeatMode(newMode)

		return isRepeatMode
	}

	override val isRepeatMode get() = playbackManager.state.repeatMode.value != RepeatMode.NONE

	override val isAudioPlayerInitialized: Boolean = true
	override val isShuffleMode: Boolean
		get() = playbackManager.state.playbackOrder.value != PlaybackOrder.DEFAULT

	override val currentAudioQueue = ItemRowAdapter(
		context,
		emptyList(),
		CardPresenter(true, @Suppress("MagicNumber") 140),
		null,
		QueryType.StaticAudioQueueItems
	)

	override val managedAudioQueue get() = currentAudioQueue

	private val audioListeners = mutableListOf<AudioEventListener>()
	private var audioListenersJob: Job? = null

	override fun addAudioEventListener(listener: AudioEventListener) {
		audioListeners.add(listener)

		if (audioListenersJob == null) {
			audioListenersJob = ProcessLifecycleOwner.get().lifecycleScope.launch {
				watchPlaybackStateChanges()
			}
		}
	}

	private suspend fun watchPlaybackStateChanges() = coroutineScope {
		playbackManager.state.playState.onEach { playState ->
			notifyListeners {
				val firstItem = currentAudioQueue.get(0) as? BaseRowItem
				firstItem?.playing = playState == PlayState.PLAYING

				onPlaybackStateChange(when (playState) {
					PlayState.STOPPED -> PlaybackController.PlaybackState.IDLE
					PlayState.PLAYING -> PlaybackController.PlaybackState.PLAYING
					PlayState.PAUSED -> PlaybackController.PlaybackState.PAUSED
					PlayState.ERROR -> PlaybackController.PlaybackState.ERROR
				}, currentAudioItem)
			}
		}.launchIn(this)

		launch {
			while (true) {
				notifyListeners {
					onProgress(playbackManager.state.positionInfo.active.inWholeMilliseconds)
				}
				delay(@Suppress("MagicNumber") 100)
			}
		}

		playbackManager.state.queue.current.onEach {
			notifyListeners {
				onQueueStatusChanged(hasAudioQueueItems())
			}
		}.launchIn(this)

		playbackManager.state.queue.entry.onEach { updateAdapter() }.launchIn(this)
	}

	private fun updateAdapter() {
		// Get all items as BaseRowItem
		val items = queue
			.items
			// Map to audio queue items
			.mapIndexed { index, item ->
				AudioQueueItem(index, item).apply {
					playing = playbackManager.state.queue.entryIndex.value == index
				}
			}
			// Remove items before currently playing item
			.drop(max(0, playbackManager.state.queue.entryIndex.value))

		// Update item row
		currentAudioQueue.replaceAll(
			items,
			areItemsTheSame = { old, new -> (old as AudioQueueItem).baseItem == (new as AudioQueueItem).baseItem },
			// The equals functions for BaseRowItem only compare by id
			areContentsTheSame = { _, _ -> false },
		)

		notifyListeners { onQueueReplaced() }
	}

	private fun notifyListeners(body: AudioEventListener.() -> Unit) {
		for (audioListener in audioListeners) {
			audioListener.body()
		}
	}

	override fun removeAudioEventListener(listener: AudioEventListener) {
		audioListeners.remove(listener)

		if (audioListeners.isEmpty()) {
			audioListenersJob?.cancel()
			audioListenersJob = null
		}
	}

	override fun queueAudioItem(item: BaseItemDto) {
		addToAudioQueue(listOf(item))
	}

	override fun clearAudioQueue() {
		playbackManager.state.stop()
	}

	override fun clearAudioQueue(releasePlayer: Boolean) {
		playbackManager.state.stop()
	}

	override fun addToAudioQueue(items: List<BaseItemDto>) {
		if (items.isEmpty()) return

		val addIndex = when (playbackManager.state.playState.value) {
			PlayState.PLAYING -> playbackManager.state.queue.entryIndex.value + 1
			else -> 0
		}

		queue.items.addAll(addIndex, items)

		if (
			playbackManager.state.queue.current.value != queue ||
			playbackManager.state.playState.value != PlayState.PLAYING
		) {
			playbackManager.state.setPlaybackOrder(if (isShuffleMode) PlaybackOrder.SHUFFLE else PlaybackOrder.DEFAULT)
			playbackManager.state.play(queue)
		}

		updateAdapter()
	}

	override fun removeFromAudioQueue(ndx: Int) {
		// Disallow removing currently playing item (legacy UI cannot keep up)
		if (playbackManager.state.queue.entryIndex.value == ndx) return

		queue.items.removeAt(ndx)
		updateAdapter()
	}

	override val isPlayingAudio: Boolean
		get() = playbackManager.state.playState.value != PlayState.STOPPED

	override fun playNow(context: Context, items: List<BaseItemDto>, position: Int, shuffle: Boolean) {
		val filteredItems = items.drop(position)
		queue.items.clear()
		queue.items.addAll(filteredItems)
		playbackManager.state.setPlaybackOrder(if (shuffle) PlaybackOrder.SHUFFLE else PlaybackOrder.DEFAULT)
		playbackManager.state.play(queue)

		navigationRepository.navigate(Destinations.nowPlaying)
	}

	override fun playNow(context: Context, items: List<BaseItemDto>, shuffle: Boolean) {
		playNow(context, items, 0, shuffle)
	}

	override fun playNow(context: Context, item: BaseItemDto) {
		playNow(context, listOf(item), 0, false)
	}

	override fun playFrom(ndx: Int): Boolean = runBlocking {
		playbackManager.state.queue.setIndex(ndx) != null
	}

	override fun shuffleAudioQueue() {
		val newMode = when (playbackManager.state.playbackOrder.value) {
			PlaybackOrder.DEFAULT -> PlaybackOrder.SHUFFLE
			else -> PlaybackOrder.DEFAULT
		}

		playbackManager.state.setPlaybackOrder(newMode)
	}

	override val nextAudioItem: BaseItemDto?
		get() = runBlocking { (playbackManager.state.queue.peekNext() as? BaseItemDtoUserQueueEntry)?.baseItem }

	override val prevAudioItem: BaseItemDto?
		get() = runBlocking { (playbackManager.state.queue.peekPrevious() as? BaseItemDtoUserQueueEntry)?.baseItem }

	override fun hasNextAudioItem(): Boolean = runBlocking {
		playbackManager.state.queue.peekNext() != null
	}

	override fun hasPrevAudioItem(): Boolean = playbackManager.state.queue.entryIndex.value > 0

	override fun nextAudioItem(): Int {
		runBlocking { playbackManager.state.queue.next() }
		notifyListeners { onQueueStatusChanged(hasAudioQueueItems()) }

		return playbackManager.state.queue.entryIndex.value
	}

	override fun prevAudioItem(): Int {
		runBlocking { playbackManager.state.queue.previous() }
		notifyListeners { onQueueStatusChanged(hasAudioQueueItems()) }

		return playbackManager.state.queue.entryIndex.value
	}

	override fun stopAudio(releasePlayer: Boolean) {
		playbackManager.state.stop()
	}

	override fun pauseAudio() {
		playbackManager.state.pause()
	}

	override fun playPauseAudio() {
		if (playbackManager.state.playState.value != PlayState.PAUSED) pauseAudio()
		else resumeAudio()
	}

	override fun resumeAudio() {
		playbackManager.state.unpause()
	}

	override fun fastForward() {
		playbackManager.state.fastForward()
	}

	override fun rewind() {
		playbackManager.state.rewind()
	}

	private class BaseItemQueue(
		private val api: ApiClient,
	) : Queue {
		val items = mutableListOf<BaseItemDto>()

		override val size: Int
			get() = items.size

		override suspend fun getItem(index: Int): QueueEntry? {
			val item = items.getOrNull(index) ?: return null
			return BaseItemDtoUserQueueEntry.build(api, item)
		}
	}
}
