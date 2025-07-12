package org.jellyfin.androidtv.ui.playback

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.ui.itemhandling.AudioQueueBaseRowItem
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem

class AudioQueueBaseRowAdapter(
	private val playbackManager: PlaybackManager,
	lifecycleScope: LifecycleCoroutineScope,
) : MutableObjectAdapter<AudioQueueBaseRowItem>(CardPresenter(true, @Suppress("MagicNumber") 140)) {
	init {
		lifecycleScope.launch {
			updateAdapter()
			watchPlaybackStateChanges()
		}
	}

	private suspend fun watchPlaybackStateChanges() = coroutineScope {
		playbackManager.queue.entry.onEach { updateAdapter() }.launchIn(this)
		playbackManager.queue.entries.onEach { updateAdapter() }.launchIn(this)
		playbackManager.state.playbackOrder.onEach { updateAdapter() }.launchIn(this)
	}

	private fun updateAdapter() {
		val currentItem = playbackManager.queue.entry.value?.let(::AudioQueueBaseRowItem)?.apply {
			playing = true
		}

		// It's safe to run this blocking as all items are prefetched via the [BaseItemQueueSupplier]
		val upcomingItems = runBlocking { playbackManager.queue.peekNext(100) }
			.mapIndexedNotNull { index, item -> item.takeIf { it.baseItem != null }?.let(::AudioQueueBaseRowItem) }

		val items = listOfNotNull(currentItem) + upcomingItems

		// Update item row
		replaceAll(
			items,
			areItemsTheSame = { old, new -> old.baseItem?.id == new.baseItem?.id },
			// The equals functions for BaseRowItem only compare by id
			areContentsTheSame = { _, _ -> false },
		)
	}
}
