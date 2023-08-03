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
	override suspend fun getStream(queueEntry: QueueEntry): MediaStream? {
		if (queueEntry !is BaseItemDtoUserQueueEntry) return null
		if (queueEntry.baseItem.type != BaseItemKind.AUDIO) return null

		val mediaInfo = getPlaybackInfo(queueEntry.baseItem)

		val url = api.audioApi.getAudioStreamUrl(
			itemId = queueEntry.baseItem.id,
			mediaSourceId = mediaInfo.mediaSource.id,
			playSessionId = mediaInfo.playSessionId,
			static = true,
		)

		return MediaStream(
			identifier = mediaInfo.playSessionId,
			queueEntry = queueEntry,
			url = url,
		)
	}
}
