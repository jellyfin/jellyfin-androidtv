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
import org.jellyfin.androidtv.ui.itemhandling.AudioQueueBaseRowItem
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
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.core.queue.supplier.QueueSupplier
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.queue.createBaseItemQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaType
import kotlin.math.max

@Suppress("TooManyFunctions")
class RewriteMediaManager(
	context: Context,
	api: ApiClient,
	private val navigationRepository: NavigationRepository,
	private val playbackManager: PlaybackManager,
) : MediaManager {
	private val queueSupplier = BaseItemQueueSupplier(api)

	override fun hasAudioQueueItems(): Boolean = currentAudioQueue.size() > 0 && currentAudioItem != null

	override val currentAudioQueueSize: Int
		get() = currentAudioQueue.size()

	override val currentAudioQueuePosition: Int
		get() = if ((playbackManager.queue.entryIndex.value) >= 0) 0 else -1

	override val currentAudioPosition: Long
		get() = playbackManager.state.positionInfo.active.inWholeMilliseconds

	override val currentAudioQueueDisplayPosition: String
		get() = (currentAudioQueuePosition + 1).toString()

	override val currentAudioQueueDisplaySize: String
		get() = playbackManager.queue.estimatedSize.toString()

	override val currentAudioItem: BaseItemDto?
		get() = playbackManager.queue.entry.value?.baseItem
			?.takeIf { it.mediaType == MediaType.AUDIO }

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
				val firstItem = currentAudioQueue.get(0) as? AudioQueueBaseRowItem
				firstItem?.playing = playState == PlayState.PLAYING

				onPlaybackStateChange(
					when (playState) {
						PlayState.STOPPED -> PlaybackController.PlaybackState.IDLE
						PlayState.PLAYING -> PlaybackController.PlaybackState.PLAYING
						PlayState.PAUSED -> PlaybackController.PlaybackState.PAUSED
						PlayState.ERROR -> PlaybackController.PlaybackState.ERROR
					}, currentAudioItem
				)
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

		playbackManager.queue.entry.onEach { entry ->
			val baseItem = entry?.baseItem
			notifyListeners {
				onQueueStatusChanged(baseItem?.mediaType == MediaType.AUDIO)
			}
		}.launchIn(this)

		playbackManager.queue.entry.onEach { updateAdapter() }.launchIn(this)
	}

	private fun updateAdapter() {
		// Get all items as BaseRowItem
		val items = queueSupplier
			.items
			// Map to audio queue items
			.mapIndexed { index, item ->
				AudioQueueBaseRowItem(item).apply {
					playing = playbackManager.queue.entryIndex.value == index
				}
			}
			// Remove items before currently playing item
			.drop(max(0, playbackManager.queue.entryIndex.value))

		// Update item row
		currentAudioQueue.replaceAll(
			items,
			areItemsTheSame = { old, new -> (old as? AudioQueueBaseRowItem)?.baseItem == (new as? AudioQueueBaseRowItem)?.baseItem },
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

	override fun addToAudioQueue(items: List<BaseItemDto>) {
		if (items.isEmpty()) return

		queueSupplier.items.addAll(items)

		if (playbackManager.state.playState.value != PlayState.PLAYING) {
			playbackManager.state.setPlaybackOrder(if (isShuffleMode) PlaybackOrder.SHUFFLE else PlaybackOrder.DEFAULT)
			playbackManager.queue.clear()
			playbackManager.queue.addSupplier(queueSupplier)
			playbackManager.state.play()
		}

		updateAdapter()
	}

	override fun removeFromAudioQueue(item: BaseItemDto) {
		val index = queueSupplier.items.indexOf(item)
		if (index == -1) return

		// Disallow removing currently playing item (legacy UI cannot keep up)
		if (playbackManager.queue.entryIndex.value == index) return

		queueSupplier.items.removeAt(index)
		updateAdapter()
	}

	override val isPlayingAudio: Boolean
		get() = playbackManager.state.playState.value == PlayState.PLAYING

	override fun playNow(context: Context, items: List<BaseItemDto>, position: Int, shuffle: Boolean) {
		val filteredItems = items.drop(position)
		queueSupplier.items.clear()
		queueSupplier.items.addAll(filteredItems)
		playbackManager.state.setPlaybackOrder(if (shuffle) PlaybackOrder.SHUFFLE else PlaybackOrder.DEFAULT)
		playbackManager.queue.clear()
		playbackManager.queue.addSupplier(queueSupplier)
		playbackManager.state.play()

		navigationRepository.navigate(Destinations.nowPlaying)
	}

	override fun playFrom(item: BaseItemDto): Boolean {
		val index = queueSupplier.items.indexOf(item)
		if (index == -1) return false
		return runBlocking {
			playbackManager.queue.setIndex(index) != null
		}
	}

	override fun shuffleAudioQueue() {
		val newMode = when (playbackManager.state.playbackOrder.value) {
			PlaybackOrder.DEFAULT -> PlaybackOrder.SHUFFLE
			else -> PlaybackOrder.DEFAULT
		}

		playbackManager.state.setPlaybackOrder(newMode)
	}

	override fun hasNextAudioItem(): Boolean = runBlocking {
		playbackManager.queue.peekNext() != null
	}

	override fun hasPrevAudioItem(): Boolean = playbackManager.queue.entryIndex.value > 0

	override fun nextAudioItem(): Int {
		runBlocking { playbackManager.queue.next() }
		notifyListeners { onQueueStatusChanged(hasAudioQueueItems()) }

		return playbackManager.queue.entryIndex.value
	}

	override fun prevAudioItem(): Int {
		runBlocking { playbackManager.queue.previous() }
		notifyListeners { onQueueStatusChanged(hasAudioQueueItems()) }

		return playbackManager.queue.entryIndex.value
	}

	override fun stopAudio(releasePlayer: Boolean) {
		playbackManager.state.stop()
	}

	override fun togglePlayPause() {
		val playState = playbackManager.state.playState.value
		if (playState == PlayState.PAUSED) playbackManager.state.unpause()
		else if (playState == PlayState.PLAYING) playbackManager.state.pause()
	}

	/**
	 * A simple [QueueSupplier] implementation for compatibility with existing UI/playback code. It contains
	 * a mutable BaseItemDto list that is used to retrieve items from.
	 */
	class BaseItemQueueSupplier(
		private val api: ApiClient,
	) : QueueSupplier {
		val items = mutableListOf<BaseItemDto>()

		override val size: Int
			get() = items.size

		override suspend fun getItem(index: Int): QueueEntry? {
			val item = items.getOrNull(index) ?: return null
			return createBaseItemQueueEntry(api, item)
		}
	}
}
