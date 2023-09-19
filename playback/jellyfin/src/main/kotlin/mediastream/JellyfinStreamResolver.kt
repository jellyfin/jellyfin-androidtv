package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.playback.core.mediastream.MediaStreamResolver
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.PlaybackInfoDto

abstract class JellyfinStreamResolver(
	private val api: ApiClient,
	private val profile: DeviceProfile,
) : MediaStreamResolver {
	data class MediaInfo(
		val playSessionId: String,
		val mediaSource: MediaSourceInfo,
	)

	protected suspend fun getPlaybackInfo(
		item: BaseItemDto,
		mediaSource: MediaSourceInfo? = null,
	): MediaInfo {
		val response by api.mediaInfoApi.getPostedPlaybackInfo(
			itemId = item.id,
			data = PlaybackInfoDto(
				userId = api.userId,
				maxStreamingBitrate = profile.maxStreamingBitrate,
				mediaSourceId = mediaSource?.id,
				liveStreamId = mediaSource?.liveStreamId,
				deviceProfile = profile,
				enableDirectPlay = true,
				enableDirectStream = true,
				enableTranscoding = true,
				allowVideoStreamCopy = true,
				allowAudioStreamCopy = true,
				autoOpenLiveStream = true,
			)
		)

		if (response.errorCode != null) {
			error("Failed to get media info for item ${item.id} source ${mediaSource?.id}: ${response.errorCode}")
		}

		val responseMediaSource = when (mediaSource) {
			null -> response.mediaSources.firstOrNull()
			else -> response.mediaSources.firstOrNull { it.id === mediaSource.id }
		}

		requireNotNull(responseMediaSource) {
			"Failed to get media info for item ${item.id} source ${mediaSource?.id}: media source missing in response"
		}

		return MediaInfo(
			playSessionId = response.playSessionId.orEmpty(),
			mediaSource = responseMediaSource
		)
	}
}
