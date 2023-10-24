package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.playback.core.mediastream.BasicMediaStream
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.mediastream.MediaStreamContainer
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.queue.item.QueueEntry
import org.jellyfin.playback.core.support.PlaySupportReport
import org.jellyfin.playback.jellyfin.queue.item.BaseItemDtoUserQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.api.client.extensions.dynamicHlsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.DeviceProfile

class AudioMediaStreamResolver(
	val api: ApiClient,
	val profile: DeviceProfile,
) : JellyfinStreamResolver(api, profile) {
	companion object {
		private val REMUX_CONTAINERS = arrayOf("mp3", "ogg", "mkv")
		private const val REMUX_SEGMENT_CONTAINER = "mp3"
	}

	private fun MediaInfo.getDirectPlayStream() = BasicMediaStream(
		identifier = playSessionId,
		conversionMethod = MediaConversionMethod.None,
		container = getMediaStreamContainer(),
		tracks = getTracks()
	)

	private fun MediaInfo.getRemuxStream(container: String) = BasicMediaStream(
		identifier = playSessionId,
		conversionMethod = MediaConversionMethod.Remux,
		container = MediaStreamContainer(
			format = container
		),
		tracks = getTracks()
	)

	private fun MediaInfo.getTranscodeStream() = BasicMediaStream(
		identifier = playSessionId,
		conversionMethod = MediaConversionMethod.Transcode,
		// The server doesn't provide us with the transcode information os we return mock data
		container = MediaStreamContainer(format = "unknown"),
		tracks = emptyList()
	)

	override suspend fun getStream(
		queueEntry: QueueEntry,
		testStream: (stream: MediaStream) -> PlaySupportReport,
	): PlayableMediaStream? {
		if (queueEntry !is BaseItemDtoUserQueueEntry) return null
		if (queueEntry.baseItem.type != BaseItemKind.AUDIO) return null

		val mediaInfo = getPlaybackInfo(queueEntry.baseItem)

		// Test for direct play support
		val directPlayStream = mediaInfo.getDirectPlayStream()
		if (testStream(directPlayStream).canPlay) {
			return directPlayStream.toPlayableMediaStream(
				queueEntry = queueEntry,
				url = api.audioApi.getAudioStreamUrl(
					itemId = queueEntry.baseItem.id,
					mediaSourceId = mediaInfo.mediaSource.id,
					playSessionId = mediaInfo.playSessionId,
					static = true,
				)
			)
		}

		// Try remuxing
		if (mediaInfo.mediaSource.supportsDirectStream) {
			for (container in REMUX_CONTAINERS) {
				val remuxStream = mediaInfo.getRemuxStream(container)
				if (testStream(remuxStream).canPlay) {
					return remuxStream.toPlayableMediaStream(
						queueEntry = queueEntry,
						url = api.audioApi.getAudioStreamByContainerUrl(
							itemId = queueEntry.baseItem.id,
							mediaSourceId = mediaInfo.mediaSource.id,
							playSessionId = mediaInfo.playSessionId,
							container = container,
						)
					)
				}
			}
		}

		// Fallback to provided transcode
		if (mediaInfo.mediaSource.supportsTranscoding) {
			val transcodeStream = mediaInfo.getTranscodeStream()

			// Skip testing transcode stream because we lack the information to do so
			return transcodeStream.toPlayableMediaStream(
				queueEntry = queueEntry,
				url = api.dynamicHlsApi.getMasterHlsAudioPlaylistUrl(
					itemId = queueEntry.baseItem.id,
					mediaSourceId = requireNotNull(mediaInfo.mediaSource.id),
					playSessionId = mediaInfo.playSessionId,
					tag = mediaInfo.mediaSource.eTag,
					segmentContainer = REMUX_SEGMENT_CONTAINER,
				)
			)
		}

		// Unable to find a suitable stream, return
		return null
	}
}
