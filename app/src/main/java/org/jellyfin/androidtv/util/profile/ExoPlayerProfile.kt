package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.util.profile.ProfileHelper.audioDirectPlayProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.deviceAV1CodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.deviceAVCCodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.deviceAVCLevelCodecProfiles
import org.jellyfin.androidtv.util.profile.ProfileHelper.deviceHevcCodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.deviceHevcLevelCodecProfiles
import org.jellyfin.androidtv.util.profile.ProfileHelper.maxAudioChannelsCodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.maxResolutionCodecProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.photoDirectPlayProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.subtitleProfile
import org.jellyfin.androidtv.util.profile.ProfileHelper.supportsHevc
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
	downMixAudio: Boolean
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
					if (supportsHevc) add(Codec.Video.HEVC)
					add(Codec.Video.H264)
				}.joinToString(",")
				audioCodec = when (downMixAudio) {
					true -> downmixSupportedAudioCodecs
					false -> allSupportedAudioCodecsWithoutFFmpegExperimental
				}.joinToString(",")
				protocol = "hls"
				copyTimestamps = false
				enableSubtitlesInManifest = true
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
			add(deviceAVCCodecProfile)
			addAll(deviceAVCLevelCodecProfiles)
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
			add(maxResolutionCodecProfile)
			// Audio channel profile
			add(maxAudioChannelsCodecProfile(channels = if (downMixAudio) 2 else 8))
		}.toTypedArray()

		subtitleProfiles = buildList {
			// Rendering supported
			arrayOf(
				Codec.Subtitle.SRT,
				Codec.Subtitle.SUBRIP,
				Codec.Subtitle.PGS,
				Codec.Subtitle.PGSSUB,
				Codec.Subtitle.DVBSUB,
				Codec.Subtitle.VTT,
				Codec.Subtitle.IDX,
			).forEach { codec ->
				add(subtitleProfile(codec, SubtitleDeliveryMethod.Embed))
				add(subtitleProfile(codec, SubtitleDeliveryMethod.Hls))
				add(subtitleProfile(codec, SubtitleDeliveryMethod.External))
			}

			// Require baking
			arrayOf(
				Codec.Subtitle.ASS,
				Codec.Subtitle.SSA,
				Codec.Subtitle.DVDSUB,
			).forEach { codec ->
				add(subtitleProfile(codec, SubtitleDeliveryMethod.Encode))
			}
		}.toTypedArray()
	}
}
