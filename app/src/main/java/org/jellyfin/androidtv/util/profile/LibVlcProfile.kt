package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.CodecTypes
import org.jellyfin.androidtv.constant.ContainerTypes
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.androidtv.util.profile.ProfileHelper.audioDirectPlayProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.deviceHevcCodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.h264VideoLevelProfileCondition
import org.jellyfin.androidtv.util.profile.ProfileHelper.h264VideoProfileCondition
import org.jellyfin.androidtv.util.profile.ProfileHelper.maxAudioChannelsCodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.photoDirectPlayProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.subtitleProfile
import org.jellyfin.apiclient.model.dlna.CodecProfile
import org.jellyfin.apiclient.model.dlna.CodecType
import org.jellyfin.apiclient.model.dlna.ContainerProfile
import org.jellyfin.apiclient.model.dlna.DirectPlayProfile
import org.jellyfin.apiclient.model.dlna.DlnaProfileType
import org.jellyfin.apiclient.model.dlna.ProfileCondition
import org.jellyfin.apiclient.model.dlna.ProfileConditionType
import org.jellyfin.apiclient.model.dlna.ProfileConditionValue
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod

class LibVlcProfile(
	isLiveTV: Boolean = false
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
					ContainerTypes.AVI,
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
			audioDirectPlayProfile(arrayOf(
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
				)),
			// Photo direct play
			photoDirectPlayProfile
		)

		codecProfiles = arrayOf(
			// HEVC profile
			deviceHevcCodecProfile,
			// H264 profile
			CodecProfile().apply {
				type = CodecType.Video
				codec = CodecTypes.H264
				conditions = arrayOf(
					h264VideoProfileCondition,
					h264VideoLevelProfileCondition
				)
			},
			// Audio channel profile
			maxAudioChannelsCodecProfile(channels = 8)
		)

		containerProfiles = arrayOf(
			ContainerProfile().apply {
				type = DlnaProfileType.Video
				container = ContainerTypes.AVI
				conditions = arrayOf(
					ProfileCondition(
						ProfileConditionType.NotEquals,
						ProfileConditionValue.VideoCodecTag,
						ContainerTypes.XVID
					)
				)
			}
		)

		subtitleProfiles = arrayOf(
			subtitleProfile("srt", SubtitleDeliveryMethod.External),
			subtitleProfile("srt", SubtitleDeliveryMethod.Embed),
			subtitleProfile("subrip", SubtitleDeliveryMethod.Embed),
			subtitleProfile("ass", SubtitleDeliveryMethod.Embed),
			subtitleProfile("ssa", SubtitleDeliveryMethod.Embed),
			subtitleProfile("pgs", SubtitleDeliveryMethod.Embed),
			subtitleProfile("pgssub", SubtitleDeliveryMethod.Embed),
			subtitleProfile("dvdsub", SubtitleDeliveryMethod.Embed),
			subtitleProfile("vtt", SubtitleDeliveryMethod.Embed),
			subtitleProfile("sub", SubtitleDeliveryMethod.Embed),
			subtitleProfile("smi", SubtitleDeliveryMethod.Embed),
			subtitleProfile("idx", SubtitleDeliveryMethod.Embed)
		)
	}
}
