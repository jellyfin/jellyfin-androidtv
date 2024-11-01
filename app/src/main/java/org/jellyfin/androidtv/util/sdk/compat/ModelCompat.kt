@file:JvmName("ModelCompat")

package org.jellyfin.androidtv.util.sdk.compat

import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.VideoRange
import org.jellyfin.sdk.model.api.VideoRangeType
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod as LegacySubtitleDeliveryMethod
import org.jellyfin.apiclient.model.dto.MediaSourceInfo as LegacyMediaSourceInfo
import org.jellyfin.apiclient.model.dto.MediaSourceType as LegacyMediaSourceType
import org.jellyfin.apiclient.model.entities.IsoType as LegacyIsoType
import org.jellyfin.apiclient.model.entities.MediaStream as LegacyMediaStream
import org.jellyfin.apiclient.model.entities.MediaStreamType as LegacyMediaStreamType
import org.jellyfin.apiclient.model.entities.Video3DFormat as LegacyVideo3DFormat
import org.jellyfin.apiclient.model.entities.VideoType as LegacyVideoType
import org.jellyfin.apiclient.model.mediainfo.MediaProtocol as LegacyMediaProtocol
import org.jellyfin.apiclient.model.mediainfo.TransportStreamTimestamp as LegacyTransportStreamTimestamp
import org.jellyfin.sdk.model.api.IsoType as ModernIsoType
import org.jellyfin.sdk.model.api.MediaProtocol as ModernMediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo as ModernMediaSourceInfo
import org.jellyfin.sdk.model.api.MediaSourceType as ModernMediaSourceType
import org.jellyfin.sdk.model.api.MediaStream as ModernMediaStream
import org.jellyfin.sdk.model.api.MediaStreamType as ModernMediaStreamType
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod as ModernSubtitleDeliveryMethod
import org.jellyfin.sdk.model.api.TransportStreamTimestamp as ModernTransportStreamTimestamp
import org.jellyfin.sdk.model.api.Video3dFormat as ModernVideo3dFormat
import org.jellyfin.sdk.model.api.VideoType as ModernVideoType

fun LegacyVideo3DFormat.asSdk(): ModernVideo3dFormat = when (this) {
	LegacyVideo3DFormat.HalfSideBySide -> ModernVideo3dFormat.HALF_SIDE_BY_SIDE
	LegacyVideo3DFormat.FullSideBySide -> ModernVideo3dFormat.FULL_SIDE_BY_SIDE
	LegacyVideo3DFormat.FullTopAndBottom -> ModernVideo3dFormat.FULL_TOP_AND_BOTTOM
	LegacyVideo3DFormat.HalfTopAndBottom -> ModernVideo3dFormat.HALF_TOP_AND_BOTTOM
	LegacyVideo3DFormat.MVC -> ModernVideo3dFormat.MVC
}

fun LegacyMediaSourceInfo.asSdk(): ModernMediaSourceInfo = ModernMediaSourceInfo(
	protocol = this.protocol.asSdk(),
	id = this.id,
	path = this.path,
	encoderPath = null, // this.encoderPath
	encoderProtocol = null, // this.encoderProtocol
	type = this.type.asSdk(),
	container = this.container,
	size = this.size,
	name = this.name,
	isRemote = this.isRemote,
	eTag = this.eTag,
	runTimeTicks = this.runTimeTicks,
	readAtNativeFramerate = this.readAtNativeFramerate,
	ignoreDts = false, // this.ignoreDts
	ignoreIndex = false, // this.ignoreIndex
	genPtsInput = false, // this.genPtsInput
	supportsTranscoding = this.supportsTranscoding,
	supportsDirectStream = this.supportsDirectStream,
	supportsDirectPlay = this.supportsDirectPlay,
	isInfiniteStream = this.isInfiniteStream,
	requiresOpening = this.requiresOpening,
	openToken = this.openToken,
	requiresClosing = this.requiresClosing,
	liveStreamId = this.liveStreamId,
	bufferMs = this.bufferMs,
	requiresLooping = false, // this.requiresLooping
	supportsProbing = false, // this.supportsProbing
	videoType = this.videoType?.asSdk(),
	isoType = this.isoType?.asSdk(),
	video3dFormat = this.video3DFormat?.asSdk(),
	mediaStreams = this.mediaStreams?.map { it.asSdk() },
	mediaAttachments = null, // this.mediaAttachments
	formats = this.formats,
	bitrate = this.bitrate,
	timestamp = this.timestamp?.asSdk(),
	requiredHttpHeaders = this.requiredHttpHeaders,
	transcodingUrl = this.transcodingUrl,
	transcodingSubProtocol = MediaStreamProtocol.fromName(this.transcodingSubProtocol.lowercase()),
	transcodingContainer = this.transcodingContainer,
	analyzeDurationMs = null, // this.analyzeDurationMs
	defaultAudioStreamIndex = this.defaultAudioStreamIndex,
	defaultSubtitleStreamIndex = this.defaultSubtitleStreamIndex,
	hasSegments = false,
)

fun LegacyMediaProtocol.asSdk(): ModernMediaProtocol = when (this) {
	LegacyMediaProtocol.File -> ModernMediaProtocol.FILE
	LegacyMediaProtocol.Http -> ModernMediaProtocol.HTTP
	LegacyMediaProtocol.Rtmp -> ModernMediaProtocol.RTMP
	LegacyMediaProtocol.Rtsp -> ModernMediaProtocol.RTSP
	LegacyMediaProtocol.Udp -> ModernMediaProtocol.UDP
	LegacyMediaProtocol.Rtp -> ModernMediaProtocol.RTP
}

fun LegacyMediaSourceType.asSdk(): ModernMediaSourceType = when (this) {
	LegacyMediaSourceType.Default -> ModernMediaSourceType.DEFAULT
	LegacyMediaSourceType.Grouping -> ModernMediaSourceType.GROUPING
	LegacyMediaSourceType.Placeholder -> ModernMediaSourceType.PLACEHOLDER
}

fun LegacyTransportStreamTimestamp.asSdk(): ModernTransportStreamTimestamp = when (this) {
	LegacyTransportStreamTimestamp.None -> ModernTransportStreamTimestamp.NONE
	LegacyTransportStreamTimestamp.Zero -> ModernTransportStreamTimestamp.ZERO
	LegacyTransportStreamTimestamp.Valid -> ModernTransportStreamTimestamp.VALID
}

fun LegacyMediaStream.asSdk(): ModernMediaStream = ModernMediaStream(
	codec = this.codec,
	codecTag = this.codecTag,
	language = this.language,
	colorRange = null, // this.colorRange
	colorSpace = null, // this.colorSpace
	colorTransfer = null, // this.colorTransfer
	colorPrimaries = null, // this.colorPrimaries
	comment = this.comment,
	timeBase = this.timeBase,
	codecTimeBase = this.codecTimeBase,
	title = this.title,
	videoRange = VideoRange.UNKNOWN, // this.videoRange
	localizedUndefined = null, // this.localizedUndefined
	localizedDefault = null, // this.localizedDefault
	localizedForced = null, // this.localizedForced
	displayTitle = this.displayTitle,
	nalLengthSize = this.nalLengthSize,
	isInterlaced = this.isInterlaced,
	isAvc = this.isAVC,
	channelLayout = this.channelLayout,
	bitRate = this.bitRate,
	bitDepth = this.bitDepth,
	refFrames = this.refFrames,
	packetLength = this.packetLength,
	channels = this.channels,
	sampleRate = this.sampleRate,
	isDefault = this.isDefault,
	isForced = this.isForced,
	height = this.height,
	width = this.width,
	averageFrameRate = this.averageFrameRate,
	realFrameRate = this.realFrameRate,
	profile = this.profile,
	type = this.type.asSdk(),
	aspectRatio = this.aspectRatio,
	index = this.index,
	score = this.score,
	isExternal = this.isExternal,
	deliveryMethod = this.deliveryMethod?.asSdk(),
	deliveryUrl = this.deliveryUrl,
	isExternalUrl = this.isExternalUrl,
	isTextSubtitleStream = this.isTextSubtitleStream,
	supportsExternalStream = this.supportsExternalStream,
	path = this.path,
	pixelFormat = this.pixelFormat,
	level = this.level,
	isAnamorphic = this.isAnamorphic,
	isHearingImpaired = false,
	videoRangeType = VideoRangeType.UNKNOWN,
)

fun LegacyMediaStreamType?.asSdk(): ModernMediaStreamType = when (this) {
	LegacyMediaStreamType.Audio -> ModernMediaStreamType.AUDIO
	LegacyMediaStreamType.Video -> ModernMediaStreamType.VIDEO
	LegacyMediaStreamType.Subtitle -> ModernMediaStreamType.SUBTITLE
	LegacyMediaStreamType.EmbeddedImage -> ModernMediaStreamType.EMBEDDED_IMAGE
	// Note: The apiclient doesn't have the DATA member and defaults to "null"
	null -> ModernMediaStreamType.DATA
}

fun LegacySubtitleDeliveryMethod.asSdk(): ModernSubtitleDeliveryMethod = when (this) {
	LegacySubtitleDeliveryMethod.Encode -> ModernSubtitleDeliveryMethod.ENCODE
	LegacySubtitleDeliveryMethod.Embed -> ModernSubtitleDeliveryMethod.EMBED
	LegacySubtitleDeliveryMethod.External -> ModernSubtitleDeliveryMethod.EXTERNAL
	LegacySubtitleDeliveryMethod.Hls -> ModernSubtitleDeliveryMethod.HLS
}

fun LegacyVideoType.asSdk(): ModernVideoType = when (this) {
	LegacyVideoType.VideoFile -> ModernVideoType.VIDEO_FILE
	LegacyVideoType.Iso -> ModernVideoType.ISO
	LegacyVideoType.Dvd -> ModernVideoType.DVD
	LegacyVideoType.BluRay -> ModernVideoType.BLU_RAY
	LegacyVideoType.HdDvd -> throw NotImplementedError("HdDvd not available in SDK")
}

fun LegacyIsoType.asSdk(): ModernIsoType = when (this) {
	LegacyIsoType.Dvd -> ModernIsoType.DVD
	LegacyIsoType.BluRay -> ModernIsoType.BLU_RAY
}
