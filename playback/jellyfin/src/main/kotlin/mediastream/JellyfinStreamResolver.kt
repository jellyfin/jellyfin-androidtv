package org.jellyfin.playback.jellyfin.mediastream

import io.ktor.http.URLBuilder
import org.jellyfin.playback.core.mediastream.MediaStreamResolver
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.MediaProtocol
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
		mediaSourceId: String? = null,
	): MediaInfo {
		val response by api.mediaInfoApi.getPostedPlaybackInfo(
			itemId = item.id,
			data = PlaybackInfoDto(
				maxStreamingBitrate = profile.maxStreamingBitrate,
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

		val mediaSource = response.mediaSources
			// Filter out invalid streams (like strm files)
			.filter { it.protocol == MediaProtocol.FILE && !it.isRemote }
			// Select first media source
			.firstOrNull { mediaSourceId == null || it.id == mediaSourceId }

		requireNotNull(mediaSource) {
			"Failed to get media info for item ${item.id} source ${mediaSourceId}: media source missing in response"
		}

		return MediaInfo(
			playSessionId = response.playSessionId.orEmpty(),
			mediaSource = mediaSource
		)
	}

	protected fun String.appendAccessToken() = let {
		URLBuilder(it)
			.apply { parameters.append("ApiKey", api.accessToken.orEmpty()) }
			.buildString()
	}
}
