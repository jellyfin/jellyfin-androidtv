package org.jellyfin.playback.core.mediastream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.PlayerState
import org.jellyfin.playback.core.backend.BackendService
import timber.log.Timber

interface MediaStreamState {
	val current: StateFlow<MediaStream?>
	val next: StateFlow<MediaStream?>
}

class DefaultMediaStreamState(
	state: PlayerState,
	coroutineScope: CoroutineScope,
	private val mediaStreamResolvers: Collection<MediaStreamResolver>,
	private val backendService: BackendService,
) : MediaStreamState {
	private val _current = MutableStateFlow<MediaStream?>(null)
	override val current: StateFlow<MediaStream?> get() = _current.asStateFlow()

	private val _next = MutableStateFlow<MediaStream?>(null)
	override val next: StateFlow<MediaStream?> get() = _next.asStateFlow()

	init {
		state.queue.entry.onEach { entry ->
			Timber.d("Queue entry changed to $entry")

			if (entry == null) {
				setCurrent(null)
			} else {
				val streamResult = runCatching {
					mediaStreamResolvers.firstNotNullOfOrNull { resolver -> resolver.getStream(entry) }
				}
				val stream = streamResult.getOrNull()
				when {
					streamResult.isFailure -> Timber.e(streamResult.exceptionOrNull(), "Media stream resolver failed for $entry")
					stream == null -> Timber.e("Unable to resolve stream for entry $entry")
					else -> {
						if (!canPlayStream(stream)) {
							Timber.w("Playback of the received media stream for $entry is not supported")
						}

						setCurrent(stream)
					}
				}
			}
		}.launchIn(coroutineScope)

		// TODO Register some kind of event when $current item is at -30 seconds to setNext()
	}

	private suspend fun canPlayStream(stream: MediaStream) = withContext(Dispatchers.Main) {
		backendService.backend?.supportsStream(stream)?.canPlay == true
	}

	private suspend fun setCurrent(stream: MediaStream?) {
		Timber.d("Current stream changed to $stream")
		val backend = requireNotNull(backendService.backend)

		_current.value = stream

		withContext(Dispatchers.Main) {
			if (stream == null) backend.stop()
			else backend.playStream(stream)
		}
	}

	private suspend fun setNext(stream: MediaStream) {
		val backend = requireNotNull(backendService.backend)

		_current.value = stream

		withContext(Dispatchers.Main) {
			backend.prepareStream(stream)
		}
	}
}
