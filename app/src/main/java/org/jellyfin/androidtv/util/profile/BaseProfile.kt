package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.CodecTypes
import org.jellyfin.androidtv.constant.SubtitleTypes
import org.jellyfin.androidtv.constant.ContainerTypes
import org.jellyfin.androidtv.util.profile.ProfileHelper.subtitleProfile
import org.jellyfin.apiclient.model.dlna.DeviceProfile
import org.jellyfin.apiclient.model.dlna.DlnaProfileType
import org.jellyfin.apiclient.model.dlna.EncodingContext
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod
import org.jellyfin.apiclient.model.dlna.TranscodingProfile

@Suppress("MagicNumber")
open class BaseProfile : DeviceProfile() {
	init {
		name = "AndroidTV"
		maxStreamingBitrate = 20_000_000 // 20 mbps
		maxStaticBitrate = 10_000_0000 // 10 mbps

		transcodingProfiles = arrayOf(
			// MKV video profile
			TranscodingProfile().apply {
				type = DlnaProfileType.Video
				context = EncodingContext.Streaming
				container = ContainerTypes.MKV
				videoCodec = CodecTypes.H264
				audioCodec = arrayOf(CodecTypes.AAC, CodecTypes.MP3).joinToString(",")
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
			subtitleProfile(SubtitleTypes.SRT, SubtitleDeliveryMethod.External),
			subtitleProfile(SubtitleTypes.SUBRIP, SubtitleDeliveryMethod.External),
			subtitleProfile(SubtitleTypes.ASS, SubtitleDeliveryMethod.Encode),
			subtitleProfile(SubtitleTypes.SSA, SubtitleDeliveryMethod.Encode),
			subtitleProfile(SubtitleTypes.PGS, SubtitleDeliveryMethod.Encode),
			subtitleProfile(SubtitleTypes.PGSSUB, SubtitleDeliveryMethod.Encode),
			subtitleProfile(SubtitleTypes.DVDSUB, SubtitleDeliveryMethod.External),
			subtitleProfile(SubtitleTypes.VTT, SubtitleDeliveryMethod.External),
			subtitleProfile(SubtitleTypes.SUB, SubtitleDeliveryMethod.External),
			subtitleProfile(SubtitleTypes.IDX, SubtitleDeliveryMethod.External)
		)
	}
}
