package org.jellyfin.playback.jellyfin.mediasegment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.core.timedevent.TimedEvent
import org.jellyfin.playback.core.timedevent.timedEvents
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.mediaSegmentsApi
import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.api.MediaSegmentType
import org.jellyfin.sdk.model.extensions.ticks

class MediaSegmentService(
	private val api: ApiClient,
	private val skipTypes: Set<MediaSegmentType>,
) : PlayerService() {
	companion object {
		const val TIMED_EVENT_PREFIX = "MediaSegment:"
	}

	override suspend fun onInitialize() {
		// Load media segments for an item as soon as it becomes the currently playing item
		manager.queue.entry
			.filterNotNull()
			.onEach { entry -> fetchMediaSegments(entry) }
			.onEach { entry -> createTimedEvents(entry) }
			.launchIn(coroutineScope)
	}

	private suspend fun fetchMediaSegments(entry: QueueEntry) {
		// Already has media segments!
		if (entry.mediaSegments != null) return

		// BaseItem doesn't exist
		val baseItem = entry.baseItem ?: return

		// Get via API
		val mediaSegments by api.mediaSegmentsApi.getItemSegments(baseItem.id)
		entry.mediaSegments = mediaSegments.items
	}

	private fun createTimedEvents(entry: QueueEntry) {
		val events = entry.timedEvents
			?.toMutableList()
			?: mutableListOf()

		// Remove existing media segments
		events.removeAll { event -> event.key?.startsWith(TIMED_EVENT_PREFIX) == true }

		// Add current media segments
		val mediaSegments = entry.mediaSegments.orEmpty()
		for (mediaSegment in mediaSegments) {
			for (timedEvents in mediaSegment.asTimedEvents()) {
				events.add(timedEvents)
			}
		}

		// Set new timed events
		entry.timedEvents = events.ifEmpty { null }
	}

	private fun MediaSegmentDto.asTimedEvents(): Collection<TimedEvent> {
		if (!skipTypes.contains(type)) return emptyList()

		return listOf(
			TimedEvent.Callback(
				key = "$TIMED_EVENT_PREFIX$id",
				position = startTicks.ticks,
				callback = {
					coroutineScope.launch(Dispatchers.Main) {
						manager.state.seek(endTicks.ticks)
					}
				}
			)
		)
	}
}
