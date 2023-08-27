package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.queue.item.QueueEntry
import org.jellyfin.playback.jellyfin.queue.item.BaseItemDtoUserQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.DeviceProfile

class AudioMediaStreamResolver(
	val api: ApiClient,
	val profile: DeviceProfile,
) : JellyfinStreamResolver(api, profile) {
	/**
	 * Force direct play when enabled, even when we know it will fail.
	 */
	var forceDirectPlay = false

	override suspend fun getStream(queueEntry: QueueEntry): MediaStream? {
		if (queueEntry !is BaseItemDtoUserQueueEntry) return null
		if (queueEntry.baseItem.type != BaseItemKind.AUDIO) return null

		val mediaInfo = getPlaybackInfo(queueEntry.baseItem)

		val url = when {
			// Direct play
			mediaInfo.mediaSource.supportsDirectPlay || forceDirectPlay -> {
				api.audioApi.getAudioStreamUrl(
					itemId = queueEntry.baseItem.id,
					mediaSourceId = mediaInfo.mediaSource.id,
					playSessionId = mediaInfo.playSessionId,
					static = true,
				)
			}
			// Remux (Direct stream)
			mediaInfo.mediaSource.supportsDirectStream -> {
				val container = requireNotNull(mediaInfo.mediaSource.container) {
					"MediaSource supports direct stream but container is null"
				}

				api.audioApi.getAudioStreamByContainerUrl(
					itemId = queueEntry.baseItem.id,
					mediaSourceId = mediaInfo.mediaSource.id,
					playSessionId = mediaInfo.playSessionId,
					container = container,
				)
			}
			// Transcode
			mediaInfo.mediaSource.supportsTranscoding -> {
				val url = requireNotNull(mediaInfo.mediaSource.transcodingUrl) {
					"MediaSource supports transcoding but transcodingUrl is null"
				}

				// TODO Use ignorePathParameters=true with SDK 1.5
				api.createUrl(url)
			}
			else -> error("Unable to find a suitable playback method for media")
		}

		return MediaStream(
			identifier = mediaInfo.playSessionId,
			queueEntry = queueEntry,
			url = url,
		)
	}
}
