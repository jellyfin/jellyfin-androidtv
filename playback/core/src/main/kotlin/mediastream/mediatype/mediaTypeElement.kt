package org.jellyfin.playback.core.mediastream.mediatype

import org.jellyfin.playback.core.element.ElementKey
import org.jellyfin.playback.core.element.elementFlow
import org.jellyfin.playback.core.element.requiredElement
import org.jellyfin.playback.core.queue.QueueEntry

private val mediaTypeKey = ElementKey<MediaType>("MediaType")

/**
 * Get or set the media type this [QueueEntry]. A supported backend will use this to
 * set correct audio attributes for the media.
 */
var QueueEntry.mediaType by requiredElement(mediaTypeKey) { MediaType.Unknown }

/**
 * Get the flow of [mediaType].
 * @see mediaType
 */
val QueueEntry.mediaTypeFlow by elementFlow(mediaTypeKey)
