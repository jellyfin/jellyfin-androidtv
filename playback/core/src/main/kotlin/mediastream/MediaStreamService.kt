package org.jellyfin.playback.core.mediastream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.queue
import timber.log.Timber

class MediaStreamService(
	private val mediaStreamResolvers: List<MediaStreamResolver>,
) : PlayerService() {
	override suspend fun onInitialize() {
		coroutineScope.launch {
			state.currentEntry.collect { entry ->
				if (entry != null) {
					val stream = mediaStreamResolvers.firstNotNullOfOrNull { it.getStream(entry) }
					if (stream != null) {
						withContext(Dispatchers.Main) { manager.backend.playStream(stream) }
					} else {
						Timber.e("Unable to resolve stream for entry $entry")
						// TODO go to next item
					}
				}

				// Preload next item
				// TODO: Do this once current stream is near it's end instead
				// TODO: Move to queue service
				val nextItem = manager.queue?.peekNext()
				if (nextItem != null) {
					val stream = mediaStreamResolvers.firstNotNullOfOrNull { it.getStream(nextItem) }
					if (stream != null) {
						withContext(Dispatchers.Main) { manager.backend.prepareStream(stream) }
					}
				}
			}
		}
	}
}
