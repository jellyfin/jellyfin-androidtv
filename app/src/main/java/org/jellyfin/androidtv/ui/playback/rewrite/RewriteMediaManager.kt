package org.jellyfin.androidtv.ui.playback.rewrite

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.preference.UserSettingPreferences
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
import org.jellyfin.playback.core.queue.Queue
import org.jellyfin.playback.core.queue.item.QueueEntry
import org.jellyfin.playback.jellyfin.queue.item.BaseItemDtoUserQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import kotlin.time.Duration.Companion.milliseconds

@Suppress("TooManyFunctions")
class RewriteMediaManager(
	private val context: Context,
	private val api: ApiClient,
	private val navigationRepository: NavigationRepository,
	private val userSettingPreferences: UserSettingPreferences,
	private val playbackManager: PlaybackManager,
) : MediaManager {
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
		isRepeatMode = !isRepeatMode
		// TODO
		Toast.makeText(context, "Not yet implemented", Toast.LENGTH_LONG).show()
		return isRepeatMode
	}

	override var isRepeatMode: Boolean = false
		private set

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
		launch {
			playbackManager.state.playState.collect { playState ->
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
			}
		}

		launch {
			while (true) {
				notifyListeners {
					onProgress(playbackManager.state.positionInfo.active.inWholeMilliseconds)
				}
				delay(@Suppress("MagicNumber") 100)
			}
		}

		launch {
			playbackManager.state.queue.current.collect {
				notifyListeners {
					onQueueStatusChanged(hasAudioQueueItems())
				}
			}
		}

		launch {
			playbackManager.state.queue.entry.collect {
				// Get all items as BaseRowItem
				val items = (playbackManager.state.queue.current.value as? BaseItemQueue)
					?.items
					.orEmpty()
					.run {
						val currentItemIndex = playbackManager.state.queue.entryIndex.value ?: -1
						// Drop previous items
						if (currentItemIndex >= 0) drop(currentItemIndex) else this
					}
					.map(::BaseRowItem)
					.apply {
						// Set first as playing
						if (isNotEmpty()) first().playing = true
						forEachIndexed { index, item -> item.index = index }
					}

				// Update item row
				currentAudioQueue.replaceAll(items)

				notifyListeners { onQueueReplaced() }
			}
		}
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

	override fun queueAudioItem(item: BaseItemDto?): Int {
		// TODO
		Toast.makeText(context, "queueAudioItem() - Not yet implemented", Toast.LENGTH_LONG).show()
		return 0
	}

	override fun clearAudioQueue() {
		playbackManager.state.stop()
	}

	override fun clearAudioQueue(releasePlayer: Boolean) {
		playbackManager.state.stop()
	}

	override fun addToAudioQueue(items: List<BaseItemDto>) {
		// TODO
		Toast.makeText(context, "addToAudioQueue() - Not yet implemented", Toast.LENGTH_LONG).show()
	}

	override fun removeFromAudioQueue(ndx: Int) {
		// TODO
		Toast.makeText(context, "removeFromAudioQueue() - Not yet implemented", Toast.LENGTH_LONG).show()
	}

	override val isPlayingAudio: Boolean
		get() = playbackManager.state.playState.value != PlayState.STOPPED

	override fun playNow(context: Context, items: List<BaseItemDto>, position: Int, shuffle: Boolean) {
		val filteredItems = items.drop(position)
		val queue = BaseItemQueue(filteredItems, api)
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
		playbackManager.state.rewind(userSettingPreferences[UserSettingPreferences.skipForwardLength].milliseconds)
	}

	override fun rewind() {
		playbackManager.state.rewind(userSettingPreferences[UserSettingPreferences.skipBackLength].milliseconds)
	}

	private class BaseItemQueue(
		val items: List<BaseItemDto>,
		private val api: ApiClient,
	) : Queue {
		override val size: Int
			get() = items.size

		override suspend fun getItem(index: Int): QueueEntry? {
			val item = items.getOrNull(index) ?: return null
			return BaseItemDtoUserQueueEntry.build(api, item)
		}
	}
}
