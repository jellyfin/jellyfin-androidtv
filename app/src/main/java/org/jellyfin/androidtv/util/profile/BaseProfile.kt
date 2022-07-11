package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.Codec
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
				container = Codec.Container.MKV
				videoCodec = Codec.Video.H264
				audioCodec = arrayOf(Codec.Audio.AAC, Codec.Audio.MP3).joinToString(",")
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
			subtitleProfile(Codec.Subtitle.SRT, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.SUBRIP, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.ASS, SubtitleDeliveryMethod.Encode),
			subtitleProfile(Codec.Subtitle.SSA, SubtitleDeliveryMethod.Encode),
			subtitleProfile(Codec.Subtitle.PGS, SubtitleDeliveryMethod.Encode),
			subtitleProfile(Codec.Subtitle.PGSSUB, SubtitleDeliveryMethod.Encode),
			subtitleProfile(Codec.Subtitle.DVDSUB, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.VTT, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.WEBVTT, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.SUB, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.IDX, SubtitleDeliveryMethod.External)
		)
	}
}
