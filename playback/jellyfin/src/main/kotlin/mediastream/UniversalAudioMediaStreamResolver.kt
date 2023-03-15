package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.mediastream.MediaStreamResolver
import org.jellyfin.playback.core.queue.item.QueueEntry
import org.jellyfin.playback.jellyfin.queue.item.BaseItemDtoUserQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.universalAudioApi
import org.jellyfin.sdk.model.api.BaseItemKind

class UniversalAudioMediaStreamResolver(
	val api: ApiClient,
) : MediaStreamResolver {
	override suspend fun getStream(queueEntry: QueueEntry): MediaStream? {
		if (queueEntry !is BaseItemDtoUserQueueEntry) return null
		if (queueEntry.baseItem.type != BaseItemKind.AUDIO) return null

		val mediaSource = queueEntry.baseItem.mediaSources?.firstOrNull()

		val url = api.universalAudioApi.getUniversalAudioStreamUrl(
			itemId = queueEntry.baseItem.id,
			mediaSourceId = mediaSource?.id,
			enableRedirection = false,
			enableRemoteMedia = false,
			// Add containers here for direct play - everything else is transcoded
			container = setOf(
				"opus", "webm|opus", "mp3", "aac", "m4a|aac", "m4b|aac",
				"flac", "webma", "webm|webma", "wav", "ogg"
			),
			// Transcoding fallback
			transcodingContainer = "mp3",
			transcodingProtocol = "http",
			audioCodec = "mp3",
		)

		return MediaStream(queueEntry = queueEntry, url = url)
	}
}
