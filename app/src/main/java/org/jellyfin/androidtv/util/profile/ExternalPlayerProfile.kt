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
			},
			DirectPlayProfile().apply {
				type = DlnaProfileType.Audio
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
			subtitleProfile(Codec.Subtitle.SUB, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.IDX, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.SMI, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.SMIL, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.TTML, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.WEBVTT, SubtitleDeliveryMethod.Embed),

			subtitleProfile(Codec.Subtitle.SRT, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.SUBRIP, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.ASS, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.SSA, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.PGS, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.PGSSUB, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.DVDSUB, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.VTT, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.SUB, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.IDX, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.SMI, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.SMIL, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.TTML, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.WEBVTT, SubtitleDeliveryMethod.External)
		)
	}
}
