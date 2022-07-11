package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.util.profile.ProfileHelper.subtitleProfile
import org.jellyfin.apiclient.model.dlna.DeviceProfile
import org.jellyfin.apiclient.model.dlna.DirectPlayProfile
import org.jellyfin.apiclient.model.dlna.DlnaProfileType
import org.jellyfin.apiclient.model.dlna.EncodingContext
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod
import org.jellyfin.apiclient.model.dlna.TranscodingProfile

@Suppress("MagicNumber")
class ExternalPlayerProfile : DeviceProfile() {
	init {
		name = "AndroidTV-External"
		maxStaticBitrate = 100_000_000 // 100 mbps

		directPlayProfiles = arrayOf(
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
					Codec.Container.DVR_MS,
					Codec.Container.WTV
				).joinToString(",")
			}
		)

		transcodingProfiles = arrayOf(
			// MKV video profile
			TranscodingProfile().apply {
				type = DlnaProfileType.Video
				context = EncodingContext.Streaming
				container = Codec.Container.MKV
				videoCodec = Codec.Video.H264
				audioCodec = arrayOf(
					Codec.Audio.AAC,
					Codec.Audio.MP3,
					Codec.Audio.AC3
				).joinToString(",")
				copyTimestamps = true
			},
			// MP3 audio profile
			TranscodingProfile().apply {
				type = DlnaProfileType.Audio
				context = EncodingContext.Streaming
				container = Codec.Container.MP3
				audioCodec = Codec.Audio.MP3
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
			subtitleProfile(Codec.Subtitle.IDX, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.SMI, SubtitleDeliveryMethod.Embed)
		)
	}
}
