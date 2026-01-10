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
import org.jellyfin.androidtv.integration.dream.visibleInScreensaver
import org.jellyfin.androidtv.ui.playback.AudioEventListener
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackController
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

@Suppress("TooManyFunctions")
class RewriteMediaManager(
	private val api: ApiClient,
	private val playbackManager: PlaybackManager,
) : MediaManager {
	override fun hasAudioQueueItems(): Boolean = playbackManager.queue.estimatedSize > 0 && currentAudioItem != null

	override val currentAudioQueueSize: Int
		get() = playbackManager.queue.estimatedSize

	override val currentAudioQueuePosition: Int
		get() = if ((playbackManager.queue.entryIndex.value) >= 0) 0 else -1

	override val currentAudioPosition: Long
		get() = playbackManager.state.positionInfo.active.inWholeMilliseconds

	override val currentAudioQueueDisplayPosition: String
		get() = (playbackManager.queue.entryIndex.value + 1).toString()

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
					onProgress(playbackManager.state.positionInfo.active.inWholeMilliseconds, playbackManager.state.positionInfo.duration.inWholeMilliseconds)
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

		playbackManager.queue.entry.onEach { notifyListeners { onQueueReplaced() } }.launchIn(this)
		playbackManager.state.playbackOrder.onEach { notifyListeners { onQueueReplaced() } }.launchIn(this)
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

		playbackManager.queue.addSupplier(BaseItemQueueSupplier(api, items, true))
		playbackManager.state.setPlaybackOrder(if (isShuffleMode) PlaybackOrder.SHUFFLE else PlaybackOrder.DEFAULT)

		if (playbackManager.state.playState.value != PlayState.PLAYING) playbackManager.state.play()
	}

	override fun removeFromAudioQueue(entry: QueueEntry) {
		runBlocking { playbackManager.queue.removeEntry(entry) }
	}

	override val isPlayingAudio: Boolean
		get() = playbackManager.state.playState.value == PlayState.PLAYING

	override fun playNow(context: Context, items: List<BaseItemDto>, position: Int, shuffle: Boolean) {
		val filteredItems = items.drop(position)

		playbackManager.state.setPlaybackOrder(if (shuffle) PlaybackOrder.SHUFFLE else PlaybackOrder.DEFAULT)
		playbackManager.queue.clear()

		if (filteredItems.isNotEmpty()) {
			playbackManager.queue.addSupplier(BaseItemQueueSupplier(api, filteredItems, true))
			playbackManager.state.play()
		}
	}

	override fun playFrom(entry: QueueEntry): Boolean {
		val index = playbackManager.queue.indexOf(entry) ?: return false
		return runBlocking { playbackManager.queue.setIndex(index) != null }
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

	override fun rewind() {
		playbackManager.state.rewind()
	}

	override fun togglePlayPause() {
		val playState = playbackManager.state.playState.value
		if (playState == PlayState.PAUSED || playState == PlayState.STOPPED) playbackManager.state.unpause()
		else if (playState == PlayState.PLAYING) playbackManager.state.pause()
	}

	override fun fastForward() {
		playbackManager.state.fastForward()
	}

	/**
	 * A simple [QueueSupplier] implementation for compatibility with existing UI/playback code. It contains
	 * a mutable BaseItemDto list that is used to retrieve items from.
	 */
	class BaseItemQueueSupplier(
		private val api: ApiClient,
		val items: List<BaseItemDto>,
		val visibleInScreensaver: Boolean,
	) : QueueSupplier {
		override val size: Int
			get() = items.size

		override suspend fun getItem(index: Int): QueueEntry? {
			val item = items.getOrNull(index) ?: return null
			return createBaseItemQueueEntry(api, item).also {
				it.visibleInScreensaver = visibleInScreensaver
			}
		}
	}
}
