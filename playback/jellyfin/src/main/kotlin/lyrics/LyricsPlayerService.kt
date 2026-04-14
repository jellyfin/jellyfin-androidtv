package org.jellyfin.playback.jellyfin.lyrics

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.lyricsApi

class LyricsPlayerService(
	private val api: ApiClient,
) : PlayerService() {
	override suspend fun onInitialize() {
		// Load lyrics for an item as soon as it becomes the currently playing item
		manager.queue.entry
			.filterNotNull()
			.onEach { entry -> fetchLyrics(entry) }
			.launchIn(coroutineScope)
	}

	private suspend fun fetchLyrics(entry: QueueEntry) {
		// Already has lyrics!
		if (entry.lyrics != null) return

		// BaseItem doesn't exist or doesn't have lyrics
		val baseItem = entry.baseItem ?: return
		if (baseItem.hasLyrics != true) return

		// Get via API
		val lyrics by api.lyricsApi.getLyrics(baseItem.id)
		entry.lyrics = lyrics
	}
}
