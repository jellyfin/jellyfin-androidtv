package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.CodecTypes
import org.jellyfin.androidtv.constant.ContainerTypes
import org.jellyfin.androidtv.constant.SubtitleTypes
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
					ContainerTypes.DVR_MS,
					ContainerTypes.WTV
				).joinToString(",")
			}
		)

		transcodingProfiles = arrayOf(
			// MKV video profile
			TranscodingProfile().apply {
				type = DlnaProfileType.Video
				context = EncodingContext.Streaming
				container = ContainerTypes.MKV
				videoCodec = CodecTypes.H264
				audioCodec = arrayOf(
					CodecTypes.AAC,
					CodecTypes.MP3,
					CodecTypes.AC3
				).joinToString(",")
				copyTimestamps = true
			},
			// MP3 audio profile
			TranscodingProfile().apply {
				type = DlnaProfileType.Audio
				context = EncodingContext.Streaming
				container = CodecTypes.MP3
				audioCodec = CodecTypes.MP3
			}
		)

		subtitleProfiles = arrayOf(
			subtitleProfile(SubtitleTypes.SRT, SubtitleDeliveryMethod.Embed),
			subtitleProfile(SubtitleTypes.SUBRIP, SubtitleDeliveryMethod.Embed),
			subtitleProfile(SubtitleTypes.ASS, SubtitleDeliveryMethod.Embed),
			subtitleProfile(SubtitleTypes.SSA, SubtitleDeliveryMethod.Embed),
			subtitleProfile(SubtitleTypes.PGS, SubtitleDeliveryMethod.Embed),
			subtitleProfile(SubtitleTypes.PGSSUB, SubtitleDeliveryMethod.Embed),
			subtitleProfile(SubtitleTypes.DVDSUB, SubtitleDeliveryMethod.Embed),
			subtitleProfile(SubtitleTypes.VTT, SubtitleDeliveryMethod.Embed),
			subtitleProfile(SubtitleTypes.SUB, SubtitleDeliveryMethod.Embed),
			subtitleProfile(SubtitleTypes.IDX, SubtitleDeliveryMethod.Embed),
			subtitleProfile(SubtitleTypes.SMI, SubtitleDeliveryMethod.Embed)
		)
	}
}
