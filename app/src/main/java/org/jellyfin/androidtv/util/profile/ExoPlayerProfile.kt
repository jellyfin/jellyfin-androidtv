package org.jellyfin.androidtv.util.profile

import android.content.Context
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.util.DeviceUtils
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.androidtv.util.profile.ProfileHelper.audioDirectPlayProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.deviceHevcCodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.h264VideoLevelProfileCondition
import org.jellyfin.androidtv.util.profile.ProfileHelper.h264VideoProfileCondition
import org.jellyfin.androidtv.util.profile.ProfileHelper.max1080pProfileConditions
import org.jellyfin.androidtv.util.profile.ProfileHelper.maxAudioChannelsCodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.photoDirectPlayProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.subtitleProfile
import org.jellyfin.apiclient.model.dlna.CodecProfile
import org.jellyfin.apiclient.model.dlna.CodecType
import org.jellyfin.apiclient.model.dlna.DirectPlayProfile
import org.jellyfin.apiclient.model.dlna.DlnaProfileType
import org.jellyfin.apiclient.model.dlna.EncodingContext
import org.jellyfin.apiclient.model.dlna.ProfileCondition
import org.jellyfin.apiclient.model.dlna.ProfileConditionType
import org.jellyfin.apiclient.model.dlna.ProfileConditionValue
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod
import org.jellyfin.apiclient.model.dlna.TranscodingProfile

class ExoPlayerProfile(
	context: Context,
	isLiveTV: Boolean = false,
	isLiveTVDirectPlayEnabled: Boolean = false,
	isAC3Enabled: Boolean = false,
) : BaseProfile() {
	private val downmixSupportedAudioCodecs = arrayOf(
		Codec.Audio.AAC,
		Codec.Audio.MP3,
		Codec.Audio.MP2
	)

	/**
	 * Returns all audio codecs used commonly in video containers.
	 * This does not include containers / codecs found in audio files
	 */
	private val allSupportedAudioCodecs = downmixSupportedAudioCodecs + arrayOf(
		Codec.Audio.AAC_LATM,
		Codec.Audio.ALAC,
		Codec.Audio.AC3,
		Codec.Audio.EAC3,
		Codec.Audio.DCA,
		Codec.Audio.DTS,
		Codec.Audio.MLP,
		Codec.Audio.TRUEHD,
		Codec.Audio.PCM_ALAW,
		Codec.Audio.PCM_MULAW,
	)

	init {
		name = "AndroidTV-ExoPlayer"

		transcodingProfiles = arrayOf(
			// TS video profile
			TranscodingProfile().apply {
				type = DlnaProfileType.Video
				this.context = EncodingContext.Streaming
				container = Codec.Container.TS
				videoCodec = buildList {
					if (deviceHevcCodecProfile.ContainsCodec(Codec.Video.HEVC, Codec.Container.TS)) add(Codec.Video.HEVC)
					add(Codec.Video.H264)
				}.joinToString(",")
				audioCodec = buildList {
					if (isAC3Enabled) add(Codec.Audio.AC3)
					add(Codec.Audio.AAC)
					add(Codec.Audio.MP3)
				}.joinToString(",")
				protocol = "hls"
				copyTimestamps = false
			},
			// MP3 audio profile
			TranscodingProfile().apply {
				type = DlnaProfileType.Audio
				this.context = EncodingContext.Streaming
				container = Codec.Container.MP3
				audioCodec = Codec.Audio.MP3
			}
		)

		directPlayProfiles = buildList {
			// Video direct play
			if (!isLiveTV || isLiveTVDirectPlayEnabled) {
				add(DirectPlayProfile().apply {
					type = DlnaProfileType.Video

					container = listOfNotNull(
						if (isLiveTV) Codec.Container.TS else null,
						if (isLiveTV) Codec.Container.MPEGTS else null,
						Codec.Container.M4V,
						Codec.Container.MOV,
						Codec.Container.XVID,
						Codec.Container.VOB,
						Codec.Container.MKV,
						Codec.Container.WMV,
						Codec.Container.ASF,
						Codec.Container.OGM,
						Codec.Container.OGV,
						Codec.Container.MP4,
						Codec.Container.WEBM
					).joinToString(",")

					videoCodec = arrayOf(
						Codec.Video.H264,
						Codec.Video.HEVC,
						Codec.Video.VP8,
						Codec.Video.VP9,
						Codec.Video.MPEG,
						Codec.Video.MPEG2VIDEO
					).joinToString(",")

					audioCodec = when {
						Utils.downMixAudio(context) -> downmixSupportedAudioCodecs
						else -> allSupportedAudioCodecs
					}.joinToString(",")
				})
			}
			// Audio direct play
			add(audioDirectPlayProfile(allSupportedAudioCodecs + arrayOf(
				Codec.Audio.MPA,
				Codec.Audio.FLAC,
				Codec.Audio.WAV,
				Codec.Audio.WMA,
				Codec.Audio.OGG,
				Codec.Audio.OGA,
				Codec.Audio.WEBMA,
				Codec.Audio.APE,
				Codec.Audio.OPUS,
			)))
			// Photo direct play
			add(photoDirectPlayProfile)
		}.toTypedArray()

		codecProfiles = buildList {
			// H264 profile
			add(CodecProfile().apply {
				type = CodecType.Video
				codec = Codec.Video.H264
				conditions = buildList {
					add(h264VideoProfileCondition)
					add(h264VideoLevelProfileCondition)
					if (!DeviceUtils.has4kVideoSupport()) addAll(max1080pProfileConditions)
				}.toTypedArray()
			})
			// H264 ref frames profile
			add(CodecProfile().apply {
				type = CodecType.Video
				codec = Codec.Video.H264
				conditions = arrayOf(
					ProfileCondition(
						ProfileConditionType.LessThanEqual,
						ProfileConditionValue.RefFrames,
						"12"
					)
				)
				applyConditions = arrayOf(
					ProfileCondition(
						ProfileConditionType.GreaterThanEqual,
						ProfileConditionValue.Width,
						"1200"
					)
				)
			})
			// H264 ref frames profile
			add(CodecProfile().apply {
				type = CodecType.Video
				codec = Codec.Video.H264
				conditions = arrayOf(
					ProfileCondition(
						ProfileConditionType.LessThanEqual,
						ProfileConditionValue.RefFrames,
						"4"
					)
				)
				applyConditions = arrayOf(
					ProfileCondition(
						ProfileConditionType.GreaterThanEqual,
						ProfileConditionValue.Width,
						"1900"
					)
				)
			})
			// HEVC profile
			add(deviceHevcCodecProfile)
			// Limit video resolution support for older devices
			if (!DeviceUtils.has4kVideoSupport()) {
				add(CodecProfile().apply {
					type = CodecType.Video
					conditions = max1080pProfileConditions
				})
			}
			// Audio channel profile
			add(maxAudioChannelsCodecProfile(channels = 6))
		}.toTypedArray()

		subtitleProfiles = arrayOf(
			subtitleProfile(Codec.Subtitle.SRT, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.SRT, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.SUBRIP, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.SUBRIP, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.ASS, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.ASS, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.SSA, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.SSA, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.PGS, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.PGS, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.PGSSUB, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.PGSSUB, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.DVDSUB, SubtitleDeliveryMethod.Encode),
			subtitleProfile(Codec.Subtitle.VTT, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.VTT, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.WEBVTT, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.WEBVTT, SubtitleDeliveryMethod.External),
			subtitleProfile(Codec.Subtitle.SUB, SubtitleDeliveryMethod.Encode),
			subtitleProfile(Codec.Subtitle.IDX, SubtitleDeliveryMethod.Encode),
		)
	}
}
