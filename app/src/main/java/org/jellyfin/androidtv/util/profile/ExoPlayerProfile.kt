package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.CodecTypes
import org.jellyfin.androidtv.constant.ContainerTypes
import org.jellyfin.androidtv.util.DeviceUtils
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.androidtv.util.profile.ProfileHelper.deviceHevcCodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.subtitleProfile
import org.jellyfin.apiclient.model.dlna.CodecProfile
import org.jellyfin.apiclient.model.dlna.CodecType
import org.jellyfin.apiclient.model.dlna.DirectPlayProfile
import org.jellyfin.apiclient.model.dlna.DlnaProfileType
import org.jellyfin.apiclient.model.dlna.ProfileCondition
import org.jellyfin.apiclient.model.dlna.ProfileConditionType
import org.jellyfin.apiclient.model.dlna.ProfileConditionValue
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod

class ExoPlayerProfile(
	isLiveTV: Boolean = false,
	isLiveTVDirectPlayEnabled: Boolean = false,
	isDtsEnabled: Boolean = true
) : BaseProfile() {
	init {
		name = "AndroidTV-ExoPlayer"

		directPlayProfiles = listOfNotNull(
			// Video direct play
			if (!isLiveTV || isLiveTVDirectPlayEnabled) {
				DirectPlayProfile().apply {
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
							if (isDtsEnabled) CodecTypes.DCA else null,
							if (isDtsEnabled) CodecTypes.DTS else null
						).joinToString(",")
					}
				}
			} else {
				null
			},
			// Audio direct play
			DirectPlayProfile().apply {
				type = DlnaProfileType.Audio

				container = arrayOf(
					CodecTypes.AAC,
					CodecTypes.MP3,
					CodecTypes.MPA,
					CodecTypes.WAV,
					CodecTypes.WMA,
					CodecTypes.MP2,
					ContainerTypes.OGG,
					ContainerTypes.OGA,
					ContainerTypes.WEBMA,
					CodecTypes.APE,
					CodecTypes.OPUS
				).joinToString(",")
			},
			// Photo direct play
			DirectPlayProfile().apply {
				type = DlnaProfileType.Photo
				container = "jpg,jpeg,png,gif"
			}
		).toTypedArray()

		codecProfiles = arrayOf(
			// H264 profile
			CodecProfile().apply {
				type = CodecType.Video
				codec = CodecTypes.H264
				conditions = arrayOf(
					ProfileCondition(
						ProfileConditionType.EqualsAny,
						ProfileConditionValue.VideoProfile,
						"high|main|baseline|constrained baseline"
					),
					ProfileCondition(
						ProfileConditionType.LessThanEqual,
						ProfileConditionValue.VideoLevel,
						if (DeviceUtils.isFireTvStickGen1()) "41" else "51"
					)
				)
			},
			// H264 ref frames profile
			CodecProfile().apply {
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
			},
			// H264 ref frames profile
			CodecProfile().apply {
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
			},
			// HEVC profile
			deviceHevcCodecProfile,
			// Audio channel profile
			CodecProfile().apply {
				type = CodecType.VideoAudio
				conditions = arrayOf(
					ProfileCondition(
						ProfileConditionType.LessThanEqual,
						ProfileConditionValue.AudioChannels,
						"6"
					)
				)
			}
		)

		subtitleProfiles = arrayOf(
			subtitleProfile("srt", SubtitleDeliveryMethod.External),
			subtitleProfile("srt", SubtitleDeliveryMethod.Embed),
			subtitleProfile("subrip", SubtitleDeliveryMethod.Embed),
			subtitleProfile("ass", SubtitleDeliveryMethod.Encode),
			subtitleProfile("ssa", SubtitleDeliveryMethod.Encode),
			subtitleProfile("pgs", SubtitleDeliveryMethod.Encode),
			subtitleProfile("pbssub", SubtitleDeliveryMethod.Encode),
			subtitleProfile("dvdsub", SubtitleDeliveryMethod.Encode),
			subtitleProfile("vtt", SubtitleDeliveryMethod.Embed),
			subtitleProfile("sub", SubtitleDeliveryMethod.Embed),
			subtitleProfile("idx", SubtitleDeliveryMethod.Embed)
		)
	}
}
