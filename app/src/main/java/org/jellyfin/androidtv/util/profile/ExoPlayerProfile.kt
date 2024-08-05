package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.util.profile.ProfileHelper.audioDirectPlayProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.deviceAV1CodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.deviceHevcCodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.deviceHevcLevelCodecProfiles
import org.jellyfin.androidtv.util.profile.ProfileHelper.h264VideoLevelProfileCondition
import org.jellyfin.androidtv.util.profile.ProfileHelper.h264VideoProfileCondition
import org.jellyfin.androidtv.util.profile.ProfileHelper.max1080pProfileConditions
import org.jellyfin.androidtv.util.profile.ProfileHelper.maxAudioChannelsCodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.photoDirectPlayProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.subtitleProfile
import org.jellyfin.apiclient.model.dlna.CodecProfile
import org.jellyfin.apiclient.model.dlna.CodecType
import org.jellyfin.apiclient.model.dlna.DeviceProfile
import org.jellyfin.apiclient.model.dlna.DirectPlayProfile
import org.jellyfin.apiclient.model.dlna.DlnaProfileType
import org.jellyfin.apiclient.model.dlna.EncodingContext
import org.jellyfin.apiclient.model.dlna.ProfileCondition
import org.jellyfin.apiclient.model.dlna.ProfileConditionType
import org.jellyfin.apiclient.model.dlna.ProfileConditionValue
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod
import org.jellyfin.apiclient.model.dlna.TranscodingProfile

class ExoPlayerProfile(
	disableVideoDirectPlay: Boolean,
	isAC3Enabled: Boolean,
	downMixAudio: Boolean,
	disable4KVideo: Boolean
) : DeviceProfile() {
	private val downmixSupportedAudioCodecs = arrayOf(
		Codec.Audio.AAC,
		Codec.Audio.MP3,
		Codec.Audio.MP2
	)

	/**
	 * Returns all audio codecs used commonly in video containers.
	 * This does not include containers / codecs found in audio files
	 */
	private val allSupportedAudioCodecs = buildList {
		addAll(downmixSupportedAudioCodecs)
		add(Codec.Audio.AAC_LATM)
		add(Codec.Audio.ALAC)
		if (isAC3Enabled) add(Codec.Audio.AC3)
		if (isAC3Enabled) add(Codec.Audio.EAC3)
		add(Codec.Audio.DCA)
		add(Codec.Audio.DTS)
		add(Codec.Audio.MLP)
		add(Codec.Audio.TRUEHD)
		add(Codec.Audio.PCM_ALAW)
		add(Codec.Audio.PCM_MULAW)
		add(Codec.Audio.PCM_S16LE)
		add(Codec.Audio.PCM_S20LE)
		add(Codec.Audio.PCM_S24LE)
		add(Codec.Audio.OPUS)
		add(Codec.Audio.FLAC)
		add(Codec.Audio.VORBIS)
	}.toTypedArray()

	private val allSupportedAudioCodecsWithoutFFmpegExperimental = allSupportedAudioCodecs
		.filterNot { it == Codec.Audio.DCA || it == Codec.Audio.TRUEHD }
		.toTypedArray()

	init {
		name = "AndroidTV-ExoPlayer"

		maxStreamingBitrate = 20_000_000 // 20 mbps
		maxStaticBitrate = 10_000_0000 // 10 mbps

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
				audioCodec = when (downMixAudio) {
					true -> downmixSupportedAudioCodecs
					false -> allSupportedAudioCodecsWithoutFFmpegExperimental
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
			if (!disableVideoDirectPlay) {
				add(DirectPlayProfile().apply {
					type = DlnaProfileType.Video

					container = listOfNotNull(
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
						Codec.Container.WEBM,
						Codec.Container.TS,
						Codec.Container.HLS
					).joinToString(",")

					videoCodec = arrayOf(
						Codec.Video.H264,
						Codec.Video.HEVC,
						Codec.Video.VP8,
						Codec.Video.VP9,
						Codec.Video.MPEG,
						Codec.Video.MPEG2VIDEO,
						Codec.Video.AV1
					).joinToString(",")

					audioCodec = when (downMixAudio) {
						true -> downmixSupportedAudioCodecs
						false -> allSupportedAudioCodecs
					}.joinToString(",")
				})
			}
			// Audio direct play
			add(
				audioDirectPlayProfile(
					allSupportedAudioCodecs + arrayOf(
						Codec.Audio.MPA,
						Codec.Audio.WAV,
						Codec.Audio.WMA,
						Codec.Audio.OGG,
						Codec.Audio.OGA,
						Codec.Audio.WEBMA,
						Codec.Audio.APE,
					)
				)
			)
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
					if (disable4KVideo) addAll(max1080pProfileConditions)
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
			// HEVC profiles
			add(deviceHevcCodecProfile)
			addAll(deviceHevcLevelCodecProfiles)
			// AV1 profile
			add(deviceAV1CodecProfile)
			// Limit video resolution support for older devices
			if (disable4KVideo) {
				add(CodecProfile().apply {
					type = CodecType.Video
					conditions = max1080pProfileConditions
				})
			}
			// Audio channel profile
			add(maxAudioChannelsCodecProfile(channels = if (downMixAudio) 2 else 8))
		}.toTypedArray()

		subtitleProfiles = arrayOf(
			subtitleProfile(Codec.Subtitle.SRT, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.SUBRIP, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.ASS, SubtitleDeliveryMethod.Encode),
			subtitleProfile(Codec.Subtitle.SSA, SubtitleDeliveryMethod.Encode),
			subtitleProfile(Codec.Subtitle.PGS, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.PGSSUB, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.DVBSUB, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.DVDSUB, SubtitleDeliveryMethod.Encode),
			subtitleProfile(Codec.Subtitle.VTT, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.SUB, SubtitleDeliveryMethod.Embed),
			subtitleProfile(Codec.Subtitle.IDX, SubtitleDeliveryMethod.Embed)
		)
	}
}
