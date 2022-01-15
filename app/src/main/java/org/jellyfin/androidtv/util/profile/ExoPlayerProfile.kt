package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.CodecTypes
import org.jellyfin.androidtv.constant.ContainerTypes
import org.jellyfin.androidtv.util.DeviceUtils
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.androidtv.util.profile.ProfileHelper.audioDirectPlayProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.deviceHevcCodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.h264VideoLevelProfileCondition
import org.jellyfin.androidtv.util.profile.ProfileHelper.h264VideoProfileCondition
import org.jellyfin.androidtv.util.profile.ProfileHelper.max1080pProfileConditions
import org.jellyfin.androidtv.util.profile.ProfileHelper.maxAudioChannelsCodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.photoDirectPlayProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.subtitleProfile
import org.jellyfin.apiclient.model.dlna.*

@OptIn(ExperimentalStdlibApi::class)
class ExoPlayerProfile(
	isLiveTV: Boolean = false,
	isLiveTVDirectPlayEnabled: Boolean = false,
) : BaseProfile() {
	init {
		name = "AndroidTV-ExoPlayer"

		transcodingProfiles = arrayOf(
			// MP4 video profile
			TranscodingProfile().apply {
				type = DlnaProfileType.Video
				context = EncodingContext.Streaming
				container = ContainerTypes.MP4
				videoCodec = buildList {
					if (deviceHevcCodecProfile.ContainsCodec(CodecTypes.HEVC, ContainerTypes.MP4)) add(CodecTypes.HEVC)
					add(CodecTypes.H264)
				}.joinToString(",")
				audioCodec = arrayOf(CodecTypes.AAC, CodecTypes.MP3).joinToString(",")
				protocol = "hls"
				minSegments = 1
				copyTimestamps = false
			},
			// MP3 audio profile
			TranscodingProfile().apply {
				type = DlnaProfileType.Audio
				context = EncodingContext.Streaming
				container = CodecTypes.MP3
				audioCodec = CodecTypes.MP3
			}
		)

		directPlayProfiles = buildList {
			// Video direct play
			if (!isLiveTV || isLiveTVDirectPlayEnabled) {
				add(DirectPlayProfile().apply {
					type = DlnaProfileType.Video

					container = listOfNotNull(
						if (isLiveTV) ContainerTypes.TS else null,
						if (isLiveTV) ContainerTypes.MPEGTS else null,
						ContainerTypes.M4V,
						ContainerTypes.MOV,
						ContainerTypes.XVID,
						ContainerTypes.VOB,
						ContainerTypes.MKV,
						ContainerTypes.WMV,
						ContainerTypes.ASF,
						ContainerTypes.OGM,
						ContainerTypes.OGV,
						ContainerTypes.MP4,
						ContainerTypes.WEBM
					).joinToString(",")

					videoCodec = arrayOf(
						CodecTypes.H264,
						CodecTypes.HEVC,
						CodecTypes.VP8,
						CodecTypes.VP9,
						ContainerTypes.MPEG,
						CodecTypes.MPEG2VIDEO
					).joinToString(",")

					audioCodec = if (Utils.downMixAudio()) {
						arrayOf(
							CodecTypes.AAC,
							CodecTypes.MP3,
							CodecTypes.MP2
						).joinToString(",")
					} else {
						listOfNotNull(
							CodecTypes.AAC,
							CodecTypes.AC3,
							CodecTypes.EAC3,
							CodecTypes.AAC_LATM,
							CodecTypes.MP3,
							CodecTypes.MP2,
							CodecTypes.DCA,
							CodecTypes.DTS,
							CodecTypes.OPUS,
						).joinToString(",")
					}
				})
			}
			// Audio direct play
			add(audioDirectPlayProfile(
				CodecTypes.AAC,
				CodecTypes.MP3,
				CodecTypes.MPA,
				CodecTypes.FLAC,
				CodecTypes.WAV,
				CodecTypes.WMA,
				CodecTypes.MP2,
				ContainerTypes.OGG,
				ContainerTypes.OGA,
				ContainerTypes.WEBMA,
				CodecTypes.APE,
				CodecTypes.OPUS
			))
			// Photo direct play
			add(photoDirectPlayProfile)
		}.toTypedArray()

		codecProfiles = buildList {
			// H264 profile
			add(CodecProfile().apply {
				type = CodecType.Video
				codec = CodecTypes.H264
				conditions = buildList {
					add(h264VideoProfileCondition)
					add(h264VideoLevelProfileCondition)
					if (!DeviceUtils.has4kVideoSupport()) addAll(max1080pProfileConditions)
				}.toTypedArray()
			})
			// H264 ref frames profile
			add(CodecProfile().apply {
				type = CodecType.Video
				codec = CodecTypes.H264
				conditions = arrayOf(
					ProfileCondition(
						ProfileConditionType.LessThanEqual,
						ProfileConditionValue.RefFrames,
						"12"
					)
				)
				applyConditions = arrayOf(
					ProfileCondition(
						ProfileConditionType.GreaterThanEqual,
						ProfileConditionValue.Width,
						"1200"
					)
				)
			})
			// H264 ref frames profile
			add(CodecProfile().apply {
				type = CodecType.Video
				codec = CodecTypes.H264
				conditions = arrayOf(
					ProfileCondition(
						ProfileConditionType.LessThanEqual,
						ProfileConditionValue.RefFrames,
						"4"
					)
				)
				applyConditions = arrayOf(
					ProfileCondition(
						ProfileConditionType.GreaterThanEqual,
						ProfileConditionValue.Width,
						"1900"
					)
				)
			})
			// HEVC profile
			add(deviceHevcCodecProfile)
			// Limit video resolution support for older devices
			if (!DeviceUtils.has4kVideoSupport()) {
				add(CodecProfile().apply {
					type = CodecType.Video
					conditions = max1080pProfileConditions
				})
			}
			// Audio channel profile
			add(maxAudioChannelsCodecProfile(channels = 6))
		}.toTypedArray()

		subtitleProfiles = arrayOf(
			subtitleProfile("srt", SubtitleDeliveryMethod.External),
			subtitleProfile("srt", SubtitleDeliveryMethod.Embed),
			subtitleProfile("subrip", SubtitleDeliveryMethod.Embed),
			subtitleProfile("ass", SubtitleDeliveryMethod.Encode),
			subtitleProfile("ssa", SubtitleDeliveryMethod.Encode),
			subtitleProfile("pgs", SubtitleDeliveryMethod.Encode),
			subtitleProfile("pgssub", SubtitleDeliveryMethod.Encode),
			subtitleProfile("dvdsub", SubtitleDeliveryMethod.Encode),
			subtitleProfile("vtt", SubtitleDeliveryMethod.Embed),
			subtitleProfile("sub", SubtitleDeliveryMethod.Embed),
			subtitleProfile("idx", SubtitleDeliveryMethod.Embed)
		)
	}
}
