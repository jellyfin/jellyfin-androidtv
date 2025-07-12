package org.jellyfin.playback.core.mediastream

import org.jellyfin.playback.core.queue.QueueEntry

/**
 * Determine the media stream for a given queue item.
 */
interface MediaStreamResolver {
	/**
	 * @return [PlayableMediaStream] or null if no stream can be determined by this resolver
	 */
	suspend fun getStream(
		queueEntry: QueueEntry,
	): PlayableMediaStream?
}
