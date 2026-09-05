package org.jellyfin.androidtv.util.profile

import androidx.media3.common.MimeTypes
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.jellyfin.sdk.model.ServerVersion
import org.jellyfin.sdk.model.api.CodecType
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.api.VideoRangeType
import org.jellyfin.sdk.model.deviceprofile.DeviceProfileBuilder
import org.jellyfin.sdk.model.deviceprofile.buildDeviceProfile
import kotlin.math.roundToInt

// Video profile names as reported by the server
private const val PROFILE_NONE = "none"
private const val AVC_PROFILE_HIGH_10 = "high 10"
private const val PROFILE_MAIN = "main"
private const val PROFILE_MAIN_10 = "main 10"

private val avcBaseProfiles = listOf("high", "main", "baseline", "constrained baseline")

private val downmixSupportedAudioCodecs = arrayOf(
	Codec.Audio.AAC,
	Codec.Audio.MP2,
	Codec.Audio.MP3,
)

private val supportedAudioCodecs = arrayOf(
	Codec.Audio.AAC,
	Codec.Audio.AAC_LATM,
	Codec.Audio.AC3,
	Codec.Audio.ALAC,
	Codec.Audio.DCA,
	Codec.Audio.DTS,
	Codec.Audio.EAC3,
	Codec.Audio.FLAC,
	Codec.Audio.MLP,
	Codec.Audio.MP2,
	Codec.Audio.MP3,
	Codec.Audio.OPUS,
	Codec.Audio.PCM_ALAW,
	Codec.Audio.PCM_MULAW,
	Codec.Audio.PCM_S16LE,
	Codec.Audio.PCM_S20LE,
	Codec.Audio.PCM_S24LE,
	Codec.Audio.TRUEHD,
	Codec.Audio.VORBIS,
)

private val hlsMpegTsAudioCodecs = arrayOf(
	Codec.Audio.AAC,
	Codec.Audio.AC3,
	Codec.Audio.EAC3,
	Codec.Audio.MP3,
)

private val hlsFmp4AudioCodecs = arrayOf(
	Codec.Audio.AAC,
	Codec.Audio.AC3,
	Codec.Audio.EAC3,
	Codec.Audio.MP3,
	Codec.Audio.ALAC,
	Codec.Audio.FLAC,
	Codec.Audio.OPUS,
	Codec.Audio.DTS,
	Codec.Audio.TRUEHD,
)

private fun UserPreferences.getMaxBitrate(): Int {
	var maxBitrate = this[UserPreferences.maxBitrate].toFloatOrNull()

	// The value "0" was used in an older release, make sure we prevent that from being used to avoid video not playing
	if (maxBitrate == null || maxBitrate < 0.01f) maxBitrate = UserPreferences.maxBitrate.defaultValue.toFloat()

	// Convert megabit to bit
	return (maxBitrate * 1_000_000).roundToInt()
}

fun createDeviceProfile(
	userPreferences: UserPreferences,
	serverVersion: ServerVersion,
) = createDeviceProfile(
	mediaTest = MediaCodecCapabilitiesTest(userPreferences[UserPreferences.softwareCodecsEnabled]),
	maxBitrate = userPreferences.getMaxBitrate(),
	isAC3Enabled = userPreferences[UserPreferences.ac3Enabled],
	downMixAudio = userPreferences[UserPreferences.audioBehaviour] == AudioBehavior.DOWNMIX_TO_STEREO,
	assDirectPlay = userPreferences[UserPreferences.assDirectPlay],
	pgsDirectPlay = userPreferences[UserPreferences.pgsDirectPlay],
	userAVCLevel = userPreferences[UserPreferences.userAVCLevel].level,
	userHEVCLevel = userPreferences[UserPreferences.userHEVCLevel].level,
)

fun createDeviceProfile(
	mediaTest: MediaCodecCapabilitiesTest,
	maxBitrate: Int,
	isAC3Enabled: Boolean,
	downMixAudio: Boolean,
	assDirectPlay: Boolean,
	pgsDirectPlay: Boolean,
	userAVCLevel: Int?,
	userHEVCLevel: Int?,
) = buildDeviceProfile {
	val allowedAudioCodecs = when {
		downMixAudio -> downmixSupportedAudioCodecs
		!isAC3Enabled -> supportedAudioCodecs.filterNot { it == Codec.Audio.EAC3 || it == Codec.Audio.AC3 }.toTypedArray()
		else -> supportedAudioCodecs
	}

	val supportsHevc = mediaTest.supportsHevc()
	val supportsHevcMain10 = mediaTest.supportsHevcMain10()
	val hevcMainLevel = userHEVCLevel ?: mediaTest.getHevcMainLevel()
	val hevcMain10Level = userHEVCLevel ?: mediaTest.getHevcMain10Level()
	val supportsAVC = mediaTest.supportsAVC()
	val supportsAVCHigh10 = mediaTest.supportsAVCHigh10()
	val avcMainLevel = userAVCLevel ?: mediaTest.getAVCMainLevel()
	val avcHigh10Level = userAVCLevel ?: mediaTest.getAVCHigh10Level()
	val supportsAV1 = mediaTest.supportsAV1()
	val supportsAV1Main10 = mediaTest.supportsAV1Main10()
	val supportsVC1 = mediaTest.supportsVc1()
	val supportsVP8 = mediaTest.supportsVp8()
	val supportsVP9 = mediaTest.supportsVp9()
	val supportsMpeg2 = mediaTest.supportsMpeg2()
	val supportsMpeg4Asp = mediaTest.supportsMpeg4Asp()
	val maxResolutionAVC = mediaTest.getMaxResolution(MimeTypes.VIDEO_H264)
	val maxResolutionHevc = mediaTest.getMaxResolution(MimeTypes.VIDEO_H265)
	val maxResolutionAV1 = mediaTest.getMaxResolution(MimeTypes.VIDEO_AV1)
	val maxResolutionVC1 = mediaTest.getMaxResolution(MimeTypes.VIDEO_VC1)

	/// HDR capabilities

	// Codecs
	// AV1
	val supportsAV1DolbyVision = mediaTest.supportsAV1DolbyVision()
	val supportsAV1HDR10 = mediaTest.supportsAV1HDR10()
	val supportsAV1HDR10Plus = mediaTest.supportsAV1HDR10Plus()

	// HEVC
	val supportsHevcDolbyVision = mediaTest.supportsHevcDolbyVision()
	val supportsHevcDolbyVisionEL = mediaTest.supportsHevcDolbyVisionEL()
	val supportsHevcHDR10 = mediaTest.supportsHevcHDR10()
	val supportsHevcHDR10Plus = mediaTest.supportsHevcHDR10Plus()

	name = "AndroidTV-Default"

	/// Bitrate
	maxStaticBitrate = maxBitrate
	maxStreamingBitrate = maxBitrate

	/// Transcoding profiles
	// Video
	// The MPEG-TS profile is declared first so the server prefers it on an equal ranking; fMP4 only wins when it
	// can stream-copy an audio codec that MPEG-TS cannot carry. Keeping both is required because LiveTV tuners
	// force the "most compatible" (MPEG-TS only) profile.
	val hlsVideoCodecs = listOfNotNull(
		if (supportsHevc) Codec.Video.HEVC else null,
		Codec.Video.H264
	).toTypedArray()

	val hlsFmp4VideoCodecs = hlsVideoCodecs + listOfNotNull(
		if (supportsAV1) Codec.Video.AV1 else null,
		if (supportsVP9) Codec.Video.VP9 else null,
	)

	transcodingProfile {
		type = DlnaProfileType.VIDEO
		context = EncodingContext.STREAMING

		container = Codec.Container.TS
		protocol = MediaStreamProtocol.HLS

		videoCodec(*hlsVideoCodecs)
		audioCodec(*hlsMpegTsAudioCodecs.filter(allowedAudioCodecs::contains).toTypedArray())

		copyTimestamps = false
		enableSubtitlesInManifest = true
	}

	transcodingProfile {
		type = DlnaProfileType.VIDEO
		context = EncodingContext.STREAMING

		container = Codec.Container.MP4
		protocol = MediaStreamProtocol.HLS

		videoCodec(*hlsFmp4VideoCodecs)
		audioCodec(*hlsFmp4AudioCodecs.filter(allowedAudioCodecs::contains).toTypedArray())

		copyTimestamps = false
		enableSubtitlesInManifest = true
	}

	// Audio
	// Only the first matching audio transcoding profile is ever used by the server, so this is a single
	// profile rather than a TS/fMP4 pair like the video profiles above
	transcodingProfile {
		type = DlnaProfileType.AUDIO
		context = EncodingContext.STREAMING

		container = Codec.Container.MP4
		protocol = MediaStreamProtocol.HLS

		audioCodec(Codec.Audio.AAC)
	}

	/// Direct play profiles
	// Video
	directPlayProfile {
		type = DlnaProfileType.VIDEO

		container(
			Codec.Container.AVI,
			Codec.Container.FLV,
			Codec.Container.HLS,
			Codec.Container.M4V,
			Codec.Container.MKV,
			Codec.Container.MOV,
			Codec.Container.MP4,
			Codec.Container.MPEG,
			Codec.Container.TS,
			Codec.Container.WEBM,
		)

		videoCodec(
			Codec.Video.AV1,
			Codec.Video.H264,
			Codec.Video.HEVC,
			Codec.Video.MPEG1VIDEO,
			Codec.Video.MPEG2VIDEO,
			Codec.Video.MPEG4,
			Codec.Video.VC1,
			Codec.Video.VP8,
			Codec.Video.VP9,
		)

		audioCodec(*allowedAudioCodecs)
	}

	// Audio
	// An empty container list means "every container" to the server, which would claim formats ExoPlayer has
	// no demuxer for (asf/wma, ape, wavpack, dsd, aiff), so the supported ones are listed explicitly
	directPlayProfile {
		type = DlnaProfileType.AUDIO

		container(
			Codec.Container.AAC,
			Codec.Container.AC3,
			Codec.Container.AMR,
			Codec.Container.EAC3,
			Codec.Container.FLAC,
			Codec.Container.FLV,
			Codec.Container.HLS,
			Codec.Container.M4A,
			Codec.Container.MKV,
			Codec.Container.MOV,
			Codec.Container.MP3,
			Codec.Container.MP4,
			Codec.Container.OGG,
			Codec.Container.TS,
			Codec.Container.WAV,
			Codec.Container.WEBM,
		)

		audioCodec(*allowedAudioCodecs)
	}

	// Audio remux
	// When the container above is not supported but the codec is, the server remuxes instead of transcoding.
	for (codec in hlsFmp4AudioCodecs.filter(allowedAudioCodecs::contains)) directPlayProfile {
		type = DlnaProfileType.AUDIO

		container(Codec.Container.MP4)
		audioCodec(codec)
	}

	/// Codec profiles
	// H264 profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.H264

		conditions {
			when {
				!supportsAVC -> ProfileConditionValue.VIDEO_PROFILE equals PROFILE_NONE
				else -> ProfileConditionValue.VIDEO_PROFILE inCollection
					avcBaseProfiles + listOfNotNull(if (supportsAVCHigh10) AVC_PROFILE_HIGH_10 else null)
			}
		}
	}
	if (supportsAVC) {
		codecProfile {
			type = CodecType.VIDEO
			codec = Codec.Video.H264

			conditions {
				ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals avcMainLevel
			}

			applyConditions {
				ProfileConditionValue.VIDEO_PROFILE inCollection avcBaseProfiles
			}
		}
	}
	if (supportsAVCHigh10) {
		codecProfile {
			type = CodecType.VIDEO
			codec = Codec.Video.H264

			conditions {
				ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals avcHigh10Level
			}

			applyConditions {
				ProfileConditionValue.VIDEO_PROFILE equals AVC_PROFILE_HIGH_10
			}
		}
	}

	// H264 ref frames profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.H264

		conditions {
			ProfileConditionValue.REF_FRAMES lowerThanOrEquals 12
		}

		applyConditions {
			ProfileConditionValue.WIDTH greaterThanOrEquals 1200
		}
	}

	// H264 ref frames profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.H264

		conditions {
			ProfileConditionValue.REF_FRAMES lowerThanOrEquals 4
		}

		applyConditions {
			ProfileConditionValue.WIDTH greaterThanOrEquals 1900
		}
	}

	// HEVC profiles
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.HEVC

		conditions {
			when {
				!supportsHevc -> ProfileConditionValue.VIDEO_PROFILE equals PROFILE_NONE
				else -> ProfileConditionValue.VIDEO_PROFILE inCollection listOfNotNull(
					PROFILE_MAIN,
					if (supportsHevcMain10) PROFILE_MAIN_10 else null
				)
			}
		}
	}
	if (supportsHevc) {
		codecProfile {
			type = CodecType.VIDEO
			codec = Codec.Video.HEVC

			conditions {
				ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals hevcMainLevel
			}

			applyConditions {
				ProfileConditionValue.VIDEO_PROFILE equals PROFILE_MAIN
			}
		}
	}
	if (supportsHevcMain10) {
		codecProfile {
			type = CodecType.VIDEO
			codec = Codec.Video.HEVC

			conditions {
				ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals hevcMain10Level
			}

			applyConditions {
				ProfileConditionValue.VIDEO_PROFILE equals PROFILE_MAIN_10
			}
		}
	}

	// AV1 profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.AV1

		conditions {
			when {
				!supportsAV1 -> ProfileConditionValue.VIDEO_PROFILE equals PROFILE_NONE
				!supportsAV1Main10 -> ProfileConditionValue.VIDEO_PROFILE notEquals PROFILE_MAIN_10
				else -> ProfileConditionValue.VIDEO_PROFILE notEquals PROFILE_NONE
			}
		}
	}

	// VC1 profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.VC1

		conditions {
			when {
				!supportsVC1 -> ProfileConditionValue.VIDEO_PROFILE equals PROFILE_NONE
				else -> ProfileConditionValue.VIDEO_PROFILE notEquals PROFILE_NONE
			}
		}
	}

	// MPEG-1 profile
	// Android has no separate MPEG-1 mime type, the MPEG-2 decoder handles both
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.MPEG1VIDEO

		conditions {
			when {
				!supportsMpeg2 -> ProfileConditionValue.VIDEO_PROFILE equals PROFILE_NONE
				else -> ProfileConditionValue.VIDEO_PROFILE notEquals PROFILE_NONE
			}
		}
	}

	// MPEG-2 profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.MPEG2VIDEO

		conditions {
			when {
				!supportsMpeg2 -> ProfileConditionValue.VIDEO_PROFILE equals PROFILE_NONE
				else -> ProfileConditionValue.VIDEO_PROFILE notEquals PROFILE_NONE
			}
		}
	}

	// MPEG-4 profile
	// DivX/Xvid require Advanced Simple Profile, a Simple Profile only decoder cannot play them
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.MPEG4

		conditions {
			when {
				!supportsMpeg4Asp -> ProfileConditionValue.VIDEO_PROFILE equals PROFILE_NONE
				else -> ProfileConditionValue.VIDEO_PROFILE notEquals PROFILE_NONE
			}
		}
	}

	// VP8 profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.VP8

		conditions {
			when {
				!supportsVP8 -> ProfileConditionValue.VIDEO_PROFILE equals PROFILE_NONE
				else -> ProfileConditionValue.VIDEO_PROFILE notEquals PROFILE_NONE
			}
		}
	}

	// VP9 profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.VP9

		conditions {
			when {
				!supportsVP9 -> ProfileConditionValue.VIDEO_PROFILE equals PROFILE_NONE
				else -> ProfileConditionValue.VIDEO_PROFILE notEquals PROFILE_NONE
			}
		}
	}

	// Get max resolutions for common codecs
	// AVC
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.H264

		conditions {
			ProfileConditionValue.WIDTH lowerThanOrEquals maxResolutionAVC.width
			ProfileConditionValue.HEIGHT lowerThanOrEquals maxResolutionAVC.height
		}
	}

	// HEVC
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.HEVC

		conditions {
			ProfileConditionValue.WIDTH lowerThanOrEquals maxResolutionHevc.width
			ProfileConditionValue.HEIGHT lowerThanOrEquals maxResolutionHevc.height
		}
	}

	// AV1
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.AV1

		conditions {
			ProfileConditionValue.WIDTH lowerThanOrEquals maxResolutionAV1.width
			ProfileConditionValue.HEIGHT lowerThanOrEquals maxResolutionAV1.height
		}
	}

	// VC1
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.VC1

		conditions {
			ProfileConditionValue.WIDTH lowerThanOrEquals maxResolutionVC1.width
			ProfileConditionValue.HEIGHT lowerThanOrEquals maxResolutionVC1.height
		}
	}

	/// HDR exclude list

	val unsupportedRangeTypesAv1 = buildSet {
		add(VideoRangeType.DOVI_INVALID)

		if (!supportsAV1DolbyVision) {
			add(VideoRangeType.DOVI)
			if (!supportsAV1HDR10) add(VideoRangeType.DOVI_WITH_HDR10)
			if (!supportsAV1HDR10Plus) add(VideoRangeType.DOVI_WITH_HDR10_PLUS)
		}

		if (!supportsAV1HDR10Plus) {
			add(VideoRangeType.HDR10_PLUS)

			if (!supportsAV1HDR10) add(VideoRangeType.HDR10)
		}
	}

	val unsupportedRangeTypesHevc = buildSet {
		add(VideoRangeType.DOVI_INVALID)

		if (!supportsHevcDolbyVisionEL) {
			if (
				!KnownDefects.unreportedDoviProfile7Support ||
				!supportsHevcDolbyVision ||
				!supportsHevcMain10 ||
				!supportsHevcHDR10
			) {
				add(VideoRangeType.DOVI_WITH_EL)

				if (!supportsHevcHDR10Plus && !KnownDefects.hevcDoviHdr10PlusBug) {
					add(VideoRangeType.DOVI_WITH_ELHDR10_PLUS)
				}
			}

			if (!supportsHevcDolbyVision) {
				add(VideoRangeType.DOVI)
				if (!supportsHevcHDR10) add(VideoRangeType.DOVI_WITH_HDR10)
				if (!supportsHevcHDR10Plus && !KnownDefects.hevcDoviHdr10PlusBug) add(VideoRangeType.DOVI_WITH_HDR10_PLUS)
			}
		}

		if (!supportsHevcHDR10Plus) {
			add(VideoRangeType.HDR10_PLUS)
			if (!supportsHevcHDR10) add(VideoRangeType.HDR10)
		}

		if (KnownDefects.hevcDoviHdr10PlusBug) {
			add(VideoRangeType.DOVI_WITH_HDR10_PLUS)
			add(VideoRangeType.DOVI_WITH_ELHDR10_PLUS)
		}
	}

	// Note: The codec profiles use a workaround to create correct behavior
	// The notEquals condition will always fail the ConditionProcessor test in the server so we use applyConditions to only have the codec
	// profile be active when the media in question uses one of the unsupported range types. The server will then use the value of the
	// notEquals in the StreamBuilder to create a correct transcode pipeline

	// Codecs
	// AV1
	if (unsupportedRangeTypesAv1.isNotEmpty()) codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.AV1

		conditions {
			ProfileConditionValue.VIDEO_RANGE_TYPE notEquals unsupportedRangeTypesAv1.joinToString("|") { it.serialName }
		}

		applyConditions {
			ProfileConditionValue.VIDEO_RANGE_TYPE inCollection unsupportedRangeTypesAv1.map { it.serialName }
		}
	}

	// HEVC
	if (unsupportedRangeTypesHevc.isNotEmpty()) codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.HEVC

		conditions {
			ProfileConditionValue.VIDEO_RANGE_TYPE notEquals unsupportedRangeTypesHevc.joinToString("|") { it.serialName }
		}

		applyConditions {
			ProfileConditionValue.VIDEO_RANGE_TYPE inCollection unsupportedRangeTypesHevc.map { it.serialName }
		}
	}

	// Audio channel profile
	codecProfile {
		type = CodecType.VIDEO_AUDIO

		conditions {
			ProfileConditionValue.AUDIO_CHANNELS lowerThanOrEquals if (downMixAudio) 2 else 8
		}
	}

	/// Subtitle profiles
	// Jellyfin server only supports WebVTT subtitles in HLS, other text subtitles will be converted to WebVTT
	// which we do not want so only allow delivery over HLS for WebVTT subtitles
	subtitleProfile(Codec.Subtitle.VTT, embedded = true, hls = true, external = true)
	subtitleProfile(Codec.Subtitle.WEBVTT, embedded = true, hls = true, external = true)

	subtitleProfile(Codec.Subtitle.SRT, embedded = true, external = true)
	subtitleProfile(Codec.Subtitle.SUBRIP, embedded = true, external = true)
	subtitleProfile(Codec.Subtitle.TTML, embedded = true, external = true)

	// Not all subtitles can be loaded standalone by the player
	subtitleProfile(Codec.Subtitle.DVBSUB, embedded = true, encode = true)
	subtitleProfile(Codec.Subtitle.DVDSUB, embedded = true, encode = true)
	subtitleProfile(Codec.Subtitle.IDX, embedded = true, encode = true)
	subtitleProfile(Codec.Subtitle.PGS, embedded = pgsDirectPlay, encode = true)
	subtitleProfile(Codec.Subtitle.PGSSUB, embedded = pgsDirectPlay, encode = true)

	// ASS/SSA is supported via libass extension
	subtitleProfile(Codec.Subtitle.ASS, encode = true, embedded = assDirectPlay, external = assDirectPlay)
	subtitleProfile(Codec.Subtitle.SSA, encode = true, embedded = assDirectPlay, external = assDirectPlay)
}

// Little helper function to more easily define subtitle profiles
private fun DeviceProfileBuilder.subtitleProfile(
	format: String,
	embedded: Boolean = false,
	external: Boolean = false,
	hls: Boolean = false,
	encode: Boolean = false,
) {
	if (embedded) subtitleProfile(format, SubtitleDeliveryMethod.EMBED)
	if (external) subtitleProfile(format, SubtitleDeliveryMethod.EXTERNAL)
	if (hls) subtitleProfile(format, SubtitleDeliveryMethod.HLS)
	if (encode) subtitleProfile(format, SubtitleDeliveryMethod.ENCODE)
}
