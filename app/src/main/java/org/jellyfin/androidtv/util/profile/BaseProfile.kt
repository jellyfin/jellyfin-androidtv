package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.CodecTypes
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
			subtitleProfile("srt", SubtitleDeliveryMethod.External),
			subtitleProfile("subrip", SubtitleDeliveryMethod.External),
			subtitleProfile("ass", SubtitleDeliveryMethod.Encode),
			subtitleProfile("ssa", SubtitleDeliveryMethod.Encode),
			subtitleProfile("pgs", SubtitleDeliveryMethod.Encode),
			subtitleProfile("pgssub", SubtitleDeliveryMethod.Encode),
			subtitleProfile("dvdsub", SubtitleDeliveryMethod.External),
			subtitleProfile("vtt", SubtitleDeliveryMethod.External),
			subtitleProfile("sub", SubtitleDeliveryMethod.External),
			subtitleProfile("idx", SubtitleDeliveryMethod.External)
		)
	}
}
