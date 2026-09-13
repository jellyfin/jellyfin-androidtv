package org.jellyfin.androidtv.ui.playback

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.data.compat.PlaybackException
import org.jellyfin.androidtv.data.compat.StreamInfo
import org.jellyfin.androidtv.data.compat.VideoOptions
import org.jellyfin.androidtv.util.apiclient.Response
import org.jellyfin.androidtv.util.profile.hlsFmp4AudioCodecs
import org.jellyfin.androidtv.util.profile.hlsMpegTsAudioCodecs
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.hlsSegmentApi
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.PlaybackInfoResponse

/**
 * The server picks a transcoding container for this source (reported back as
 * [MediaSourceInfo.transcodingContainer], matched against the containers this app declares
 * support for per codec in its DeviceProfile, see deviceProfile.kt - e.g. MPEG-TS for
 * AAC/AC3/EAC3 audio, fMP4 for codecs that need it such as TrueHD/DTS) but does not apply that
 * choice to the HLS master playlist/segment URL it hands back in [MediaSourceInfo.transcodingUrl]:
 * without an explicit "SegmentContainer" query parameter that endpoint falls back to its own
 * ".ts" default regardless of what was negotiated. Passing the already-negotiated container
 * through explicitly keeps today's behavior for the common ".ts" case and fixes it for sources
 * that need a container ".ts" cannot carry, such as TrueHD or DTS passthrough.
 *
 * Container alone is not enough: without an explicit "AudioCodec" parameter either, the server
 * infers a target audio codec from the segment request's own file extension (".mp4" -> "aac"),
 * which is wrong for an HLS video segment - the extension there is the container, not the audio
 * format - and forces an unnecessary transcode even once the container is correct.
 *
 * The server's "audioCodec" parameter has its own hard 40-character limit
 * (`^[a-zA-Z0-9\-\._,|]{0,40}$`, enforced server-side), too short for the full
 * hlsFmp4AudioCodecs list from deviceProfile.kt. That list is also more than what is needed here:
 * fMP4 is only ever the negotiated container because the source needs a codec MPEG-TS cannot
 * carry in the first place (see deviceProfile.kt / StreamBuilder's ranking), so it is exactly the
 * ts-incompatible subset - hlsFmp4AudioCodecs minus hlsMpegTsAudioCodecs - that needs stating
 * explicitly for the mp4 case. The plain ts case reuses hlsMpegTsAudioCodecs as-is (well under
 * the limit already).
 */
private val fmp4OnlyAudioCodecs = hlsFmp4AudioCodecs.filterNot { it in hlsMpegTsAudioCodecs }

internal fun MediaSourceInfo.segmentContainerQueryParameters(): Map<String, Any?> {
	if (transcodingSubProtocol != MediaStreamProtocol.HLS) return emptyMap()
	val container = transcodingContainer ?: return emptyMap()

	val audioCodecs = when (container) {
		Codec.Container.MP4 -> fmp4OnlyAudioCodecs
		Codec.Container.TS -> hlsMpegTsAudioCodecs.toList()
		else -> null
	}

	return buildMap {
		put("segmentContainer", container)
		if (audioCodecs != null) put("audioCodec", audioCodecs.joinToString(","))
	}
}

private fun createStreamInfo(
	api: ApiClient,
	options: VideoOptions,
	response: PlaybackInfoResponse,
): StreamInfo = StreamInfo().apply {
	val source = response.mediaSources.firstOrNull {
		options.mediaSourceId != null && it.id == options.mediaSourceId
	} ?: response.mediaSources.firstOrNull()

	itemId = options.itemId
	mediaSource = source
	runTimeTicks = source?.runTimeTicks
	playSessionId = response.playSessionId

	if (source == null) return@apply

	if (options.enableDirectPlay && source.supportsDirectPlay) {
		playMethod = PlayMethod.DIRECT_PLAY
		container = source.container
		mediaUrl = when {
			source.isRemote && source.path != null -> source.path
			else -> api.videosApi.getVideoStreamUrl(
				itemId = itemId,
				container = container,
				mediaSourceId = source.id,
				static = true,
				tag = source.eTag,
				liveStreamId = source.liveStreamId,
			)
		}
	} else if (options.enableDirectStream && source.supportsDirectStream) {
		playMethod = PlayMethod.DIRECT_STREAM
		container = source.transcodingContainer
		mediaUrl = api.createUrl(
			requireNotNull(source.transcodingUrl),
			queryParameters = source.segmentContainerQueryParameters(),
			ignorePathParameters = true,
		)
	} else if (source.supportsTranscoding) {
		playMethod = PlayMethod.TRANSCODE
		container = source.transcodingContainer
		mediaUrl = api.createUrl(
			requireNotNull(source.transcodingUrl),
			queryParameters = source.segmentContainerQueryParameters(),
			ignorePathParameters = true,
		)
	}
}

class PlaybackManager(
	private val api: ApiClient
) {
	fun getVideoStreamInfo(
		lifecycleOwner: LifecycleOwner,
		options: VideoOptions,
		startTimeTicks: Long,
		callback: Response<StreamInfo>,
	) = lifecycleOwner.lifecycleScope.launch {
		getVideoStreamInfoInternal(options, startTimeTicks).fold(
			onSuccess = { callback.onResponse(it) },
			onFailure = { callback.onError(Exception(it)) },
		)
	}

	fun changeVideoStream(
		lifecycleOwner: LifecycleOwner,
		stream: StreamInfo,
		options: VideoOptions,
		startTimeTicks: Long,
		callback: Response<StreamInfo>
	) = lifecycleOwner.lifecycleScope.launch {
		if (stream.playSessionId != null && stream.playMethod != PlayMethod.DIRECT_PLAY) {
			withContext(Dispatchers.IO) {
				api.hlsSegmentApi.stopEncodingProcess(api.deviceInfo.id, stream.playSessionId)
			}
		}

		getVideoStreamInfoInternal(options, startTimeTicks).fold(
			onSuccess = { callback.onResponse(it) },
			onFailure = { callback.onError(Exception(it)) },
		)
	}

	private suspend fun getVideoStreamInfoInternal(
		options: VideoOptions,
		startTimeTicks: Long
	) = runCatching {
		val response = withContext(Dispatchers.IO) {
			api.mediaInfoApi.getPostedPlaybackInfo(
				itemId = requireNotNull(options.itemId) { "Item id cannot be null" },
				data = PlaybackInfoDto(
					mediaSourceId = options.mediaSourceId,
					startTimeTicks = startTimeTicks,
					deviceProfile = options.profile,
					enableDirectStream = options.enableDirectStream,
					enableDirectPlay = options.enableDirectPlay,
					maxAudioChannels = options.maxAudioChannels,
					audioStreamIndex = options.audioStreamIndex.takeIf { it != null && it >= 0 },
					subtitleStreamIndex = options.subtitleStreamIndex,
					allowVideoStreamCopy = true,
					allowAudioStreamCopy = true,
					autoOpenLiveStream = true,
					alwaysBurnInSubtitleWhenTranscoding = options.alwaysBurnInSubtitleWhenTranscoding,
				)
			).content
		}

		if (response.errorCode != null) {
			throw PlaybackException().apply {
				errorCode = response.errorCode!!
			}
		}

		createStreamInfo(api, options, response)
	}
}
