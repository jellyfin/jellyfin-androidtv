package org.jellyfin.playback.core.mediastream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import org.jellyfin.playback.core.PlayerState
import org.jellyfin.playback.core.backend.BackendService
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.queue.QueueEntry
import timber.log.Timber

internal class MediaStreamState(
	state: PlayerState,
	coroutineScope: CoroutineScope,
	private val mediaStreamResolvers: Collection<MediaStreamResolver>,
	private val backendService: BackendService,
) {
	init {
		state.queue.entry.onEach { entry ->
			Timber.d("Queue entry changed to $entry")
			val backend = requireNotNull(backendService.backend)

			if (entry == null) {
				backend.setCurrent(null)
			} else {
				val hasMediaStream = entry.ensureMediaStream(backend)

				if (hasMediaStream) {
					backend.setCurrent(entry)
				} else {
					Timber.e("Unable to resolve stream for entry $entry")

					// TODO: Somehow notify the user that we skipped an unplayable entry
					if (state.queue.peekNext() != null) {
						state.queue.next(usePlaybackOrder = true, useRepeatMode = false)
					} else {
						backend.setCurrent(null)
					}
				}
			}
		}.launchIn(coroutineScope + Dispatchers.Main)
		// TODO Register some kind of event when $current item is at -30 seconds to setNext()
	}

	private suspend fun QueueEntry.ensureMediaStream(
		backend: PlayerBackend,
	): Boolean {
		mediaStream = mediaStream ?: mediaStreamResolvers.firstNotNullOfOrNull { resolver ->
			runCatching {
				resolver.getStream(this, backend::supportsStream)
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
