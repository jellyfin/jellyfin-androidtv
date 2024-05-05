package org.jellyfin.playback.core.queue

import org.jellyfin.playback.core.element.ElementsContainer

/**
 * The QueueEntry is a single item in a queue and can represent any supported media type.
 * All related data is stored in elements via the [ElementsContainer].
 */
class QueueEntry : ElementsContainer()
