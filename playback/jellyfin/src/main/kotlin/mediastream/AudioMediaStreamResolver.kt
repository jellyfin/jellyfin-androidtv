package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.playback.core.mediastream.MediaConversionMethod
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

		val conversionMethod = when {
			// Direct play
			mediaInfo.mediaSource.supportsDirectPlay || forceDirectPlay ->  MediaConversionMethod.None
			// Remux (Direct stream)
			mediaInfo.mediaSource.supportsDirectStream ->  MediaConversionMethod.Remux
			// Transcode
			mediaInfo.mediaSource.supportsTranscoding -> MediaConversionMethod.Transcode
			else -> error("Unable to find a suitable playback method for media")
		}

		val url = when(conversionMethod) {
			// Direct play
			is MediaConversionMethod.None -> {
				api.audioApi.getAudioStreamUrl(
					itemId = queueEntry.baseItem.id,
					mediaSourceId = mediaInfo.mediaSource.id,
					playSessionId = mediaInfo.playSessionId,
					static = true,
				)
			}
			// Remux (Direct stream)
			is MediaConversionMethod.Remux -> {
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
			is MediaConversionMethod.Transcode -> {
				val url = requireNotNull(mediaInfo.mediaSource.transcodingUrl) {
					"MediaSource supports transcoding but transcodingUrl is null"
				}

				// TODO Use ignorePathParameters=true with SDK 1.5
				api.createUrl(url)
			}
		}

		return MediaStream(
			identifier = mediaInfo.playSessionId,
			queueEntry = queueEntry,
			conversionMethod = conversionMethod,
			url = url,
		)
	}
}
