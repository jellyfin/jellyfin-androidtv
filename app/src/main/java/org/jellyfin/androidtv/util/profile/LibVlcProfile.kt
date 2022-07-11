package org.jellyfin.androidtv.util.profile

import android.content.Context
import org.jellyfin.androidtv.constant.Codec
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
	context: Context,
	isLiveTV: Boolean = false,
) : BaseProfile() {
	init {
		name = "AndroidTV-libVLC"

		directPlayProfiles = arrayOf(
			// Video direct play
			DirectPlayProfile().apply {
				type = DlnaProfileType.Video

				container = arrayOf(
					Codec.Container.M4V,
					Codec.Container.`3GP`,
					Codec.Container.TS,
					Codec.Container.MPEGTS,
					Codec.Container.MOV,
					Codec.Container.XVID,
					Codec.Container.VOB,
					Codec.Container.MKV,
					Codec.Container.WMV,
					Codec.Container.ASF,
					Codec.Container.OGM,
					Codec.Container.OGV,
					Codec.Container.M2V,
					Codec.Container.AVI,
					Codec.Container.MPG,
					Codec.Container.MPEG,
					Codec.Container.MP4,
					Codec.Container.WEBM,
					Codec.Container.WTV
				).joinToString(",")

				audioCodec = listOfNotNull(
					Codec.Audio.AAC,
					Codec.Audio.MP3,
					Codec.Audio.MP2,
					Codec.Audio.AC3,
					Codec.Audio.EAC3,
					Codec.Audio.WMA,
					Codec.Audio.WMAV2,
					Codec.Audio.DCA,
					Codec.Audio.DTS,
					Codec.Audio.PCM,
					Codec.Audio.PCM_S16LE,
					Codec.Audio.PCM_S24LE,
					Codec.Audio.OPUS,
					Codec.Audio.FLAC,
					Codec.Audio.TRUEHD,
					if (!Utils.downMixAudio(context) && isLiveTV) Codec.Audio.AAC_LATM else null
				).joinToString(",")
			},
			// Audio direct play
			audioDirectPlayProfile(arrayOf(
				Codec.Audio.FLAC,
				Codec.Audio.AAC,
				Codec.Audio.MP3,
				Codec.Audio.MPA,
				Codec.Audio.WAV,
				Codec.Audio.WMA,
				Codec.Audio.MP2,
				Codec.Audio.OGG,
				Codec.Audio.OGA,
				Codec.Audio.WEBMA,
				Codec.Audio.APE
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
				codec = Codec.Video.H264
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
				container = Codec.Container.AVI
				conditions = arrayOf(
					ProfileCondition(
						ProfileConditionType.NotEquals,
						ProfileConditionValue.VideoCodecTag,
						Codec.Container.XVID
					)
				)
			}
		)

		subtitleProfiles = arrayOf(
			subtitleProfile(Codec.Subtitle.SRT, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.SUBRIP, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.ASS, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.SSA, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.PGS, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.PGSSUB, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.DVDSUB, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.VTT, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.WEBVTT, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.SUB, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.SMI, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.IDX, SubtitleDeliveryMethod.Embed)
		)
	}
}
