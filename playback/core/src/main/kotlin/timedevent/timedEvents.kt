package org.jellyfin.playback.core.timedevent

import org.jellyfin.playback.core.element.ElementKey
import org.jellyfin.playback.core.element.element
import org.jellyfin.playback.core.element.elementFlow
import org.jellyfin.playback.core.queue.QueueEntry

private val timedEventsKey = ElementKey<Collection<TimedEvent>>("TimedEvents")

/**
 * Get or set the [Collection<TimedEvent>] for this [QueueEntry].
 */
var QueueEntry.timedEvents by element(timedEventsKey)
val QueueEntry.timedEventsFlow by elementFlow(timedEventsKey)

fun QueueEntry.addTimedEvent(timedEvent: TimedEvent) {
	val timedEvents = getOrNull(timedEventsKey).orEmpty()
	val newTimedEvents = timedEvents + timedEvent
	put(timedEventsKey, newTimedEvents)
}

fun QueueEntry.removeTimedEvent(timedEvent: TimedEvent) {
	val timedEvents = getOrNull(timedEventsKey).orEmpty()
	val newTimedEvents = timedEvents - timedEvent
	if (newTimedEvents.isEmpty()) remove(timedEventsKey)
	else put(timedEventsKey, newTimedEvents)
}
