package org.jellyfin.playback.core.mediastream

import org.jellyfin.playback.core.queue.item.QueueEntry

data class MediaStream(
	val identifier: String,
	val queueEntry: QueueEntry,
	val conversionMethod: MediaConversionMethod,
	val url: String,
)
