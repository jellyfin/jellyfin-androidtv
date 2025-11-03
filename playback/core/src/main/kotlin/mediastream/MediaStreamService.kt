package org.jellyfin.playback.core.mediastream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import timber.log.Timber

internal class MediaStreamService(
	private val mediaStreamResolvers: Collection<MediaStreamResolver>,
) : PlayerService() {
	override suspend fun onInitialize() {
		manager.queue.entry.onEach { entry ->
			Timber.d("Queue entry changed to $entry")
			val backend = requireNotNull(manager.backend)

			if (entry == null) {
				backend.setCurrent(null)
			} else {
				val hasMediaStream = entry.ensureMediaStream()

				if (hasMediaStream) {
					backend.setCurrent(entry)
				} else {
					Timber.e("Unable to resolve stream for entry $entry")

					// TODO: Somehow notify the user that we skipped an unplayable entry
					if (manager.queue.peekNext() != null) {
						manager.queue.next(usePlaybackOrder = true, useRepeatMode = false)
					} else {
						backend.setCurrent(null)
					}
				}
			}
		}.launchIn(coroutineScope + Dispatchers.Main)
		// TODO Register some kind of event when $current item is at -30 seconds to setNext()
	}

	private suspend fun QueueEntry.ensureMediaStream(): Boolean {
		mediaStream = mediaStream ?: mediaStreamResolvers.firstNotNullOfOrNull { resolver ->
			runCatching {
				withContext(Dispatchers.IO) {
					resolver.getStream(this@ensureMediaStream)
				}
			}.onFailure {
				Timber.e(it, "Media stream resolver failed for $this")
			}.getOrNull()
		}

		return mediaStream != null
	}

	private fun PlayerBackend.setCurrent(item: QueueEntry?) {
		Timber.d("Current item changed to $item")

		if (item == null) stop()
		else playItem(item)
	}
}
