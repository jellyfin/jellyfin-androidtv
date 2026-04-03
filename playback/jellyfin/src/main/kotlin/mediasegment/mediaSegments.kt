package org.jellyfin.playback.jellyfin.mediasegment

import org.jellyfin.playback.core.element.ElementKey
import org.jellyfin.playback.core.element.element
import org.jellyfin.playback.core.element.elementFlow
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.sdk.model.api.MediaSegmentDto

private val mediaSegmentsKey = ElementKey<Collection<MediaSegmentDto>>("MediaSegments")

/**
 * Get or set the [Collection<MediaSegmentDto>] for this [QueueEntry].
 */
var QueueEntry.mediaSegments by element(mediaSegmentsKey)
val QueueEntry.mediaSegmentsFlow by elementFlow(mediaSegmentsKey)
