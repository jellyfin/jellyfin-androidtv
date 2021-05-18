package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.CodecTypes
import org.jellyfin.androidtv.constant.ContainerTypes
import org.jellyfin.androidtv.util.DeviceUtils
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.androidtv.util.profile.ProfileHelper.getHevcProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.getSubtitleProfile
import org.jellyfin.apiclient.model.dlna.CodecProfile
import org.jellyfin.apiclient.model.dlna.CodecType
import org.jellyfin.apiclient.model.dlna.DirectPlayProfile
import org.jellyfin.apiclient.model.dlna.DlnaProfileType
import org.jellyfin.apiclient.model.dlna.ProfileCondition
import org.jellyfin.apiclient.model.dlna.ProfileConditionType
import org.jellyfin.apiclient.model.dlna.ProfileConditionValue
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod

class LibVlcProfile(
	private var isLiveTV: Boolean = false
) : BaseProfile() {
	init {
		name = "AndroidTV-libVLC"

		directPlayProfiles = arrayOf(
			// Video direct play
			DirectPlayProfile().apply {
				type = DlnaProfileType.Video

				container = arrayOf(
					ContainerTypes.M4V,
					ContainerTypes._3GP,
					ContainerTypes.TS,
					ContainerTypes.MPEGTS,
					ContainerTypes.MOV,
					ContainerTypes.XVID,
					ContainerTypes.VOB,
					ContainerTypes.MKV,
					ContainerTypes.WMV,
					ContainerTypes.ASF,
					ContainerTypes.OGM,
					ContainerTypes.OGV,
					ContainerTypes.M2V,
					ContainerTypes.MPG,
					ContainerTypes.MPEG,
					ContainerTypes.MP4,
					ContainerTypes.WEBM,
					ContainerTypes.WTV
				).joinToString(",")

				audioCodec = listOfNotNull(
					CodecTypes.AAC,
					CodecTypes.MP3,
					CodecTypes.MP2,
					CodecTypes.AC3,
					CodecTypes.WMA,
					CodecTypes.WMAV2,
					CodecTypes.DCA,
					CodecTypes.DTS,
					CodecTypes.PCM,
					CodecTypes.PCM_S16LE,
					CodecTypes.PCM_S24LE,
					CodecTypes.OPUS,
					CodecTypes.FLAC,
					CodecTypes.TRUEHD,
					if (!Utils.downMixAudio() && isLiveTV) CodecTypes.AAC_LATM else null
				).joinToString(",")
			},
			// Audio direct play
			DirectPlayProfile().apply {
				type = DlnaProfileType.Audio

				container = arrayOf(
					CodecTypes.FLAC,
					CodecTypes.AAC,
					CodecTypes.MP3,
					CodecTypes.MPA,
					CodecTypes.WAV,
					CodecTypes.WMA,
					CodecTypes.MP2,
					ContainerTypes.OGG,
					ContainerTypes.OGA,
					ContainerTypes.WEBMA,
					CodecTypes.APE
				).joinToString(",")
			},
			// Photo direct play
			DirectPlayProfile().apply {
				type = DlnaProfileType.Photo
				container = "jpg,jpeg,png,gif"
			}
		)

		codecProfiles = arrayOf(
			// HEVC profile
			getHevcProfile(),
			// H264 profile
			CodecProfile().apply {
				type = CodecType.Video
				codec = CodecTypes.H264
				conditions = arrayOf(
					ProfileCondition(ProfileConditionType.EqualsAny, ProfileConditionValue.VideoProfile, "high|main|baseline|constrained baseline"),
					ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoLevel, if (DeviceUtils.isFireTvStickGen1()) "41" else "51"),
					ProfileCondition(ProfileConditionType.GreaterThanEqual, ProfileConditionValue.RefFrames, "2")
				)
			},
			// Audio channel profile
			CodecProfile().apply {
				type = CodecType.VideoAudio
				conditions = arrayOf(
					ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "8")
				)
			}
		)

		subtitleProfiles = arrayOf(
			getSubtitleProfile("srt", SubtitleDeliveryMethod.External),
			getSubtitleProfile("srt", SubtitleDeliveryMethod.Embed),
			getSubtitleProfile("subrip", SubtitleDeliveryMethod.Embed),
			getSubtitleProfile("ass", SubtitleDeliveryMethod.Embed),
			getSubtitleProfile("ssa", SubtitleDeliveryMethod.Embed),
			getSubtitleProfile("pgs", SubtitleDeliveryMethod.Embed),
			getSubtitleProfile("pbssub", SubtitleDeliveryMethod.Embed),
			getSubtitleProfile("dvdsub", SubtitleDeliveryMethod.Embed),
			getSubtitleProfile("vtt", SubtitleDeliveryMethod.Embed),
			getSubtitleProfile("sub", SubtitleDeliveryMethod.Embed),
			getSubtitleProfile("smi", SubtitleDeliveryMethod.Embed),
			getSubtitleProfile("idx", SubtitleDeliveryMethod.Embed)
		)
	}
}
