package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.support.PlaySupportReport
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.queue.mediaSourceId
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.universalAudioApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.MediaStreamProtocol

class UniversalAudioMediaStreamResolver(
	val api: ApiClient,
	val profile: DeviceProfile,
) : JellyfinStreamResolver(api, profile) {
	override suspend fun getStream(
		queueEntry: QueueEntry,
		testStream: (stream: MediaStream) -> PlaySupportReport,
	): PlayableMediaStream? {
		val baseItem = queueEntry.baseItem
		if (baseItem == null || baseItem.type != BaseItemKind.AUDIO) return null

		val mediaInfo = getPlaybackInfo(baseItem, queueEntry.mediaSourceId)

		val url = api.universalAudioApi.getUniversalAudioStreamUrl(
			itemId = baseItem.id,
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
			transcodingProtocol = MediaStreamProtocol.HTTP,
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
