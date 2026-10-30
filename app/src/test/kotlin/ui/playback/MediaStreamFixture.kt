package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaSourceType
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import java.util.UUID

@Suppress("LongParameterList")
internal fun video(
	language: String? = null,
	title: String? = null,
	isDefault: Boolean = false,
	isForced: Boolean = false,
	isExternal: Boolean = false,
	codec: String? = Codec.Video.H264,
): MediaStream = MediaStream(
	index = 0,
	type = MediaStreamType.VIDEO,
	language = language,
	title = title,
	codec = codec,
	isDefault = isDefault,
	isForced = isForced,
	isExternal = isExternal,
	isInterlaced = false,
	isHearingImpaired = false,
	isTextSubtitleStream = false,
	supportsExternalStream = isExternal,
)

@Suppress("LongParameterList")
internal fun audio(
	language: String? = null,
	title: String? = null,
	isDefault: Boolean = false,
	isForced: Boolean = false,
	isExternal: Boolean = false,
	codec: String? = Codec.Audio.AAC,
): MediaStream = MediaStream(
	index = 0,
	type = MediaStreamType.AUDIO,
	language = language,
	title = title,
	codec = codec,
	isDefault = isDefault,
	isForced = isForced,
	isExternal = isExternal,
	isInterlaced = false,
	isHearingImpaired = false,
	isTextSubtitleStream = false,
	supportsExternalStream = isExternal,
)

@Suppress("LongParameterList")
internal fun subtitle(
	language: String? = null,
	title: String? = null,
	isDefault: Boolean = false,
	isForced: Boolean = false,
	isExternal: Boolean = false,
	codec: String? = Codec.Subtitle.ASS,
): MediaStream = MediaStream(
	index = 0,
	type = MediaStreamType.SUBTITLE,
	language = language,
	title = title,
	codec = codec,
	isDefault = isDefault,
	isForced = isForced,
	isExternal = isExternal,
	isInterlaced = false,
	isHearingImpaired = false,
	isTextSubtitleStream = true,
	supportsExternalStream = true,
	deliveryMethod = if (isExternal) SubtitleDeliveryMethod.EXTERNAL else SubtitleDeliveryMethod.EMBED,
)

internal fun media(
	vararg streams: MediaStream,
	container: String = Codec.Container.MKV,
	kind: BaseItemKind = BaseItemKind.EPISODE,
): BaseItemDto {
	val mediaStreams = streams.toList()
	val mediaSource = MediaSourceInfo(
		id = UUID.randomUUID().toString(),
		mediaStreams = mediaStreams,
		container = container,
		type = MediaSourceType.DEFAULT,
		protocol = MediaProtocol.FILE,
		transcodingSubProtocol = MediaStreamProtocol.HTTP,
		readAtNativeFramerate = false,
		genPtsInput = false,
		hasSegments = false,
		isRemote = false,
		isInfiniteStream = false,
		ignoreDts = false,
		ignoreIndex = false,
		requiresOpening = false,
		requiresClosing = false,
		requiresLooping = false,
		supportsProbing = true,
		supportsTranscoding = true,
		supportsDirectStream = true,
		supportsDirectPlay = true,
	)

	return BaseItemDto(
		id = UUID.randomUUID(),
		type = kind,
		mediaType = MediaType.VIDEO,
		mediaStreams = mediaStreams,
		mediaSources = listOf(mediaSource),
	)
}
