package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.CodecTypes
import org.jellyfin.androidtv.constant.ContainerTypes
import org.jellyfin.androidtv.util.profile.ProfileHelper.getSubtitleProfile
import org.jellyfin.apiclient.model.dlna.DeviceProfile
import org.jellyfin.apiclient.model.dlna.DlnaProfileType
import org.jellyfin.apiclient.model.dlna.EncodingContext
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod
import org.jellyfin.apiclient.model.dlna.TranscodingProfile

class BaseProfile : DeviceProfile() {
	init {
		name = "AndroidTV"
		maxStreamingBitrate = 20000000
		maxStaticBitrate = 100000000

		transcodingProfiles = arrayOf(
			// MKV video profile
			TranscodingProfile().apply {
				type = DlnaProfileType.Video
				context = EncodingContext.Streaming
				container = ContainerTypes.MKV
				videoCodec = CodecTypes.H264
				audioCodec = arrayOf(CodecTypes.AAC, CodecTypes.MP3).joinToString()
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
			getSubtitleProfile("srt", SubtitleDeliveryMethod.External),
			getSubtitleProfile("subrip", SubtitleDeliveryMethod.External),
			getSubtitleProfile("ass", SubtitleDeliveryMethod.Encode),
			getSubtitleProfile("ssa", SubtitleDeliveryMethod.Encode),
			getSubtitleProfile("pgs", SubtitleDeliveryMethod.Encode),
			getSubtitleProfile("pbssub", SubtitleDeliveryMethod.Encode),
			getSubtitleProfile("dvdsub", SubtitleDeliveryMethod.External),
			getSubtitleProfile("vtt", SubtitleDeliveryMethod.External),
			getSubtitleProfile("sub", SubtitleDeliveryMethod.External),
			getSubtitleProfile("idx", SubtitleDeliveryMethod.External)
		)
	}
}
