package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.MediaStreamResolver
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.queue.mediaSourceId
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PlaybackInfoDto

class JellyfinMediaStreamResolver(
	private val api: ApiClient,
	private val deviceProfileBuilder: () -> DeviceProfile,
) : PlayerService(), MediaStreamResolver {
	companion object {
		private val supportedMediaTypes = arrayOf(MediaType.VIDEO, MediaType.AUDIO)
	}

	override suspend fun getStream(queueEntry: QueueEntry): PlayableMediaStream? {
		val baseItem = queueEntry.baseItem
		if (baseItem == null || !supportedMediaTypes.contains(baseItem.mediaType)) return null

		val mediaSourceId = queueEntry.mediaSourceId ?: getContinuedVersionId(queueEntry)
		val mediaInfo = getPlaybackInfo(baseItem, mediaSourceId)

		// Remember which version is actually played so it can be reported to the server and continued
		// when playing the next entry
		queueEntry.mediaSourceId = mediaInfo.mediaSource.id

		return when {
			// Direct play video
			mediaInfo.mediaSource.supportsDirectPlay && baseItem.mediaType == MediaType.VIDEO -> mediaInfo.toStream(
				queueEntry = queueEntry,
				conversionMethod = MediaConversionMethod.None,
				url = api.videosApi.getVideoStreamUrl(
					itemId = baseItem.id,
					container = mediaInfo.mediaSource.container,
					mediaSourceId = mediaInfo.mediaSource.id,
					static = true,
					tag = mediaInfo.mediaSource.eTag,
					liveStreamId = mediaInfo.mediaSource.liveStreamId,
				)
			)

			// Direct play audio
			mediaInfo.mediaSource.supportsDirectPlay && baseItem.mediaType == MediaType.AUDIO -> mediaInfo.toStream(
				queueEntry = queueEntry,
				conversionMethod = MediaConversionMethod.None,
				url = api.audioApi.getAudioStreamUrl(
					itemId = baseItem.id,
					container = mediaInfo.mediaSource.container,
					mediaSourceId = mediaInfo.mediaSource.id,
					static = true,
					tag = mediaInfo.mediaSource.eTag,
					liveStreamId = mediaInfo.mediaSource.liveStreamId,
				)
			)

			// Remux (direct stream)
			mediaInfo.mediaSource.supportsDirectStream && mediaInfo.mediaSource.transcodingUrl != null -> mediaInfo.toStream(
				queueEntry = queueEntry,
				conversionMethod = MediaConversionMethod.Remux,
				url = api.createUrl(requireNotNull(mediaInfo.mediaSource.transcodingUrl), ignorePathParameters = true)
			)

			// Transcode
			mediaInfo.mediaSource.supportsTranscoding && mediaInfo.mediaSource.transcodingUrl != null -> mediaInfo.toStream(
				queueEntry = queueEntry,
				conversionMethod = MediaConversionMethod.Transcode,
				url = api.createUrl(requireNotNull(mediaInfo.mediaSource.transcodingUrl), ignorePathParameters = true)
			)

			// No compatible stream found
			else -> null
		}
	}

	/**
	 * Id of the media source of [queueEntry] that has the same name as the version played for the entry
	 * before it, or null when it has no matching version. Keeps playback in the version that is being
	 * watched when continuing with another item.
	 */
	private fun getContinuedVersionId(queueEntry: QueueEntry): String? {
		val mediaSources = queueEntry.baseItem?.mediaSources
		if (mediaSources == null || mediaSources.size < 2) return null

		val playedVersion = getPlayedVersion(queueEntry) ?: return null

		return mediaSources.firstOrNull { it.name.equals(playedVersion.name, ignoreCase = true) }?.id
	}

	/**
	 * The media source played for the entry before [queueEntry]. That is the entry that is playing when
	 * the next entry is preloaded, or the last entry with a played version before it in the queue when
	 * playback of [queueEntry] already started.
	 */
	private fun getPlayedVersion(queueEntry: QueueEntry): MediaSourceInfo? {
		val currentEntry = manager.queue.entry.value
		val previousEntry = when {
			currentEntry != null && currentEntry !== queueEntry -> currentEntry

			else -> {
				val entries = manager.queue.entries.value
				val index = entries.indexOfFirst { it === queueEntry }
				if (index < 0) null else entries.take(index).lastOrNull { it.mediaSourceId != null }
			}
		}

		val playedId = previousEntry?.mediaSourceId ?: return null
		val mediaSources = previousEntry.baseItem?.mediaSources
		// Only continue a version when the previous entry had alternate versions to pick from
		if (mediaSources == null || mediaSources.size < 2) return null

		return mediaSources.firstOrNull { it.id == playedId }
	}

	private suspend fun getPlaybackInfo(
		item: BaseItemDto,
		mediaSourceId: String? = null,
	): MediaInfo {
		val profile = deviceProfileBuilder()
		val response by api.mediaInfoApi.getPostedPlaybackInfo(
			itemId = item.id,
			data = PlaybackInfoDto(
				mediaSourceId = mediaSourceId,
				deviceProfile = profile,
				enableDirectPlay = true,
				enableDirectStream = true,
				enableTranscoding = true,
				allowVideoStreamCopy = true,
				allowAudioStreamCopy = true,
				autoOpenLiveStream = false,
			)
		)

		if (response.errorCode != null) {
			error("Failed to get media info for item ${item.id} source ${mediaSourceId}: ${response.errorCode}")
		}

		val mediaSource = when (mediaSourceId) {
			null -> response.mediaSources
				// Filter out invalid streams (like strm files)
				.filter { it.protocol == MediaProtocol.FILE && !it.isRemote }
				// The server orders the media sources so the version that should play comes first
				.firstOrNull()

			// Always use the requested version, even when it needs to be transcoded
			else -> response.mediaSources.firstOrNull { it.id == mediaSourceId }
		}

		requireNotNull(mediaSource) {
			"Failed to get media info for item ${item.id} source ${mediaSourceId}: media source missing in response"
		}

		return MediaInfo(
			playSessionId = response.playSessionId.orEmpty(),
			mediaSource = mediaSource
		)
	}

	private fun MediaInfo.toStream(
		queueEntry: QueueEntry,
		conversionMethod: MediaConversionMethod,
		url: String,
	) = PlayableMediaStream(
		identifier = playSessionId,
		conversionMethod = conversionMethod,
		container = getMediaStreamContainer(),
		tracks = getTracks(),
		queueEntry = queueEntry,
		url = url,
	)
}
