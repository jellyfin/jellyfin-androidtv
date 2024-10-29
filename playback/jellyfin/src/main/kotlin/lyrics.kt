package org.jellyfin.playback.jellyfin

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jellyfin.playback.core.element.ElementKey
import org.jellyfin.playback.core.element.element
import org.jellyfin.playback.core.element.elementFlow
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.lyricsApi
import org.jellyfin.sdk.model.api.LyricDto

private val lyricsKey = ElementKey<LyricDto>("LyricDto")

/**
 * Get or set the [LyricDto] for this [QueueEntry].
 */
var QueueEntry.lyrics by element(lyricsKey)
val QueueEntry.lyricsFlow by elementFlow(lyricsKey)

class LyricsPlayerService(
	private val api: ApiClient,
) : PlayerService() {
	override suspend fun onInitialize() {
		// Load lyrics for an item as soon as it becomes the currently playing item
		manager.queue.entry
			.onEach { entry -> entry?.let { fetchLyrics(entry) } }
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
