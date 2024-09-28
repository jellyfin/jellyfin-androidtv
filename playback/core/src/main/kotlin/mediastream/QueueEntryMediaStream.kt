package org.jellyfin.playback.core.mediastream

import org.jellyfin.playback.core.element.ElementKey
import org.jellyfin.playback.core.element.element
import org.jellyfin.playback.core.element.elementFlow
import org.jellyfin.playback.core.queue.QueueEntry

private val mediaStreamKey = ElementKey<PlayableMediaStream>("MediaStream")

/**
 * Get or set the [MediaStream] for this [QueueEntry].
 */
var QueueEntry.mediaStream by element(mediaStreamKey)

/**
 * Get the [MediaStream] flow for this [QueueEntry].
 */
val QueueEntry.mediaStreamFlow by elementFlow(mediaStreamKey)
