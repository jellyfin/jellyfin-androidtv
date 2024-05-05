package org.jellyfin.playback.core.mediastream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import org.jellyfin.playback.core.PlayerState
import org.jellyfin.playback.core.backend.BackendService
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.queue.QueueEntry
import timber.log.Timber

interface MediaStreamState {
	val current: StateFlow<PlayableMediaStream?>
	val next: StateFlow<PlayableMediaStream?>
}

class DefaultMediaStreamState(
	state: PlayerState,
	coroutineScope: CoroutineScope,
	private val mediaStreamResolvers: Collection<MediaStreamResolver>,
	private val backendService: BackendService,
) : MediaStreamState {
	private val _current = MutableStateFlow<PlayableMediaStream?>(null)
	override val current: StateFlow<PlayableMediaStream?> get() = _current.asStateFlow()

	private val _next = MutableStateFlow<PlayableMediaStream?>(null)
	override val next: StateFlow<PlayableMediaStream?> get() = _next.asStateFlow()

	init {
		state.queue.entry.onEach { entry ->
			Timber.d("Queue entry changed to $entry")
			val backend = requireNotNull(backendService.backend)

			if (entry == null) {
				backend.setCurrent(null)
			} else {
				val stream = entry.getOrComputeMediaStream(backend)

				if (stream == null) {
					Timber.e("Unable to resolve stream for entry $entry")

					// TODO: Somehow notify the user that we skipped an unplayable entry
					if (state.queue.peekNext() != null) {
						state.queue.next(usePlaybackOrder = true, useRepeatMode = false)
					} else {
						backend.setCurrent(null)
					}
				} else {
					backend.setCurrent(stream)
				}
			}
		}.launchIn(coroutineScope + Dispatchers.Main)

		// TODO Register some kind of event when $current item is at -30 seconds to setNext()
	}

	private suspend fun QueueEntry.getOrComputeMediaStream(
		backend: PlayerBackend,
	): PlayableMediaStream? = mediaStream ?: mediaStreamResolvers.firstNotNullOfOrNull { resolver ->
		runCatching {
			resolver.getStream(this, backend::supportsStream)
		}.onFailure {
			Timber.e(it, "Media stream resolver failed for $this")
		}.getOrNull()
	}.also { mediaStream = it }

	private fun PlayerBackend.setCurrent(stream: PlayableMediaStream?) {
		Timber.d("Current stream changed to $stream")
		_current.value = stream

		if (stream == null) stop()
		else playStream(stream)
	}

	private fun PlayerBackend.setNext(stream: PlayableMediaStream) {
		_current.value = stream
		prepareStream(stream)
	}
}
