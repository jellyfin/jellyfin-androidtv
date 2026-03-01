package org.jellyfin.playback.core.mediastream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.core.timedevent.TimedEvent
import org.jellyfin.playback.core.timedevent.addTimedEvent
import org.jellyfin.playback.core.timedevent.timedEvents
import timber.log.Timber
import kotlin.time.Duration

internal class MediaStreamService(
	private val mediaStreamResolvers: Collection<MediaStreamResolver>,
	private val preloadDuration: Duration,
) : PlayerService() {
	private companion object {
		private const val TIMED_EVENT_PRELOAD = "MediaStreamServicePreloadNext"
	}

	override suspend fun onInitialize() {
		manager.queue.entry.onEach { entry ->
			Timber.d("Queue entry changed to $entry")

			if (entry == null) {
				val backend = requireNotNull(manager.backend)
				backend.stop()
			} else {
				playEntry(entry)
				entry.ensurePreloadTimedEvent()
			}
		}.launchIn(coroutineScope + Dispatchers.Main)
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

	private fun QueueEntry.ensurePreloadTimedEvent() {
		if (preloadDuration <= Duration.ZERO) return

		val hasEvent = timedEvents?.any { it.key == TIMED_EVENT_PRELOAD } == true
		if (hasEvent) return

		addTimedEvent(
			TimedEvent.Block(
				key = TIMED_EVENT_PRELOAD,
				start = -preloadDuration,
				end = Duration.INFINITE,
				onActivate = { preloadNextEntry() }
			)
		)
	}

	private suspend fun playEntry(entry: QueueEntry) {
		val backend = requireNotNull(manager.backend)
		val hasMediaStream = entry.ensureMediaStream()

		if (hasMediaStream) {
			backend.playItem(entry)
		} else {
			Timber.e("Unable to resolve stream for entry $entry")

			// TODO: Somehow notify the user that we skipped an unplayable entry
			if (manager.queue.peekNext() != null) {
				manager.queue.next(usePlaybackOrder = true, useRepeatMode = false)
			} else {
				backend.stop()
			}
		}
	}

	private fun preloadNextEntry() = coroutineScope.launch(Dispatchers.Main) {
		// Peek into the next item to preload
		val nextItem = manager.queue.peekNext() ?: return@launch

		// Preload media stream information
		val hasMediaStream = nextItem.ensureMediaStream()

		if (hasMediaStream) {
			// Preload media in backend
			val backend = requireNotNull(manager.backend)
			backend.prepareItem(nextItem)
		}
	}
}
