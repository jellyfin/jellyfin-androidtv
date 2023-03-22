package org.jellyfin.playback.core.mediastream

import org.jellyfin.playback.core.queue.item.QueueEntry

data class MediaStream(
	val queueEntry: QueueEntry,
	val url: String,
)
