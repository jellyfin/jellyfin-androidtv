package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.queue.item.QueueEntry
import org.jellyfin.playback.core.support.PlaySupportReport
import org.jellyfin.playback.jellyfin.queue.item.BaseItemDtoUserQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.universalAudioApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.DeviceProfile

class UniversalAudioMediaStreamResolver(
	val api: ApiClient,
	val profile: DeviceProfile,
) : JellyfinStreamResolver(api, profile) {
	override suspend fun getStream(
		queueEntry: QueueEntry,
		testStream: (stream: MediaStream) -> PlaySupportReport,
	): PlayableMediaStream? {
		if (queueEntry !is BaseItemDtoUserQueueEntry) return null
		if (queueEntry.baseItem.type != BaseItemKind.AUDIO) return null

		val mediaInfo = getPlaybackInfo(queueEntry.baseItem)

		val url = api.universalAudioApi.getUniversalAudioStreamUrl(
			itemId = queueEntry.baseItem.id,
			mediaSourceId = mediaInfo.mediaSource.id,
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

		return PlayableMediaStream(
			identifier = mediaInfo.playSessionId,
			queueEntry = queueEntry,
			conversionMethod = MediaConversionMethod.None,
			url = url,
			container = mediaInfo.getMediaStreamContainer(),
			tracks = mediaInfo.getTracks()
		).takeIf { stream -> testStream(stream).canPlay }
	}
}
