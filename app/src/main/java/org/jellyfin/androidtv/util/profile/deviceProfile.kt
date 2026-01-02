package org.jellyfin.androidtv.util.profile

import android.content.Context
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

private fun UserPreferences.getMaxBitrate(): Int {
	var maxBitrate = this[UserPreferences.maxBitrate].toFloatOrNull()

	// The value "0" was used in an older release, make sure we prevent that from being used to avoid video not playing
	if (maxBitrate == null || maxBitrate < 0.01f) maxBitrate = UserPreferences.maxBitrate.defaultValue.toFloat()

	// Convert megabit to bit
	return (maxBitrate * 1_000_000).roundToInt()
}

fun createDeviceProfile(
	context: Context,
	userPreferences: UserPreferences,
	serverVersion: ServerVersion,
) = createDeviceProfile(
	mediaTest = MediaCodecCapabilitiesTest(context),
	maxBitrate = userPreferences.getMaxBitrate(),
	isAC3Enabled = userPreferences[UserPreferences.ac3Enabled],
	downMixAudio = userPreferences[UserPreferences.audioBehaviour] == AudioBehavior.DOWNMIX_TO_STEREO,
	assDirectPlay = false,
	pgsDirectPlay = userPreferences[UserPreferences.pgsDirectPlay],
	jellyfinTenEleven = serverVersion >= ServerVersion(10, 11, 0),
)

fun createDeviceProfile(
	mediaTest: MediaCodecCapabilitiesTest,
	maxBitrate: Int,
	isAC3Enabled: Boolean,
	downMixAudio: Boolean,
	assDirectPlay: Boolean,
	pgsDirectPlay: Boolean,
	jellyfinTenEleven: Boolean,
) = buildDeviceProfile {
	val allowedAudioCodecs = when {
		downMixAudio -> downmixSupportedAudioCodecs
		!isAC3Enabled -> supportedAudioCodecs.filterNot { it == Codec.Audio.EAC3 || it == Codec.Audio.AC3 }.toTypedArray()
		else -> supportedAudioCodecs
	}

	val supportsHevc = mediaTest.supportsHevc()
	val supportsHevcMain10 = mediaTest.supportsHevcMain10()
	val hevcMainLevel = mediaTest.getHevcMainLevel()
	val hevcMain10Level = mediaTest.getHevcMain10Level()
	val supportsAVC = mediaTest.supportsAVC()
	val supportsAVCHigh10 = mediaTest.supportsAVCHigh10()
	val avcMainLevel = mediaTest.getAVCMainLevel()
	val avcHigh10Level = mediaTest.getAVCHigh10Level()
	val supportsAV1 = mediaTest.supportsAV1()
	val supportsAV1Main10 = mediaTest.supportsAV1Main10()
	val supportsVC1 = mediaTest.supportsVc1()
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
	transcodingProfile {
		type = DlnaProfileType.VIDEO
		context = EncodingContext.STREAMING

		container = Codec.Container.TS
		protocol = MediaStreamProtocol.HLS

		if (supportsHevc) videoCodec(Codec.Video.HEVC)
		videoCodec(Codec.Video.H264)

		audioCodec(*allowedAudioCodecs)

		copyTimestamps = false
		enableSubtitlesInManifest = true
	}

	// Audio
	transcodingProfile {
		type = DlnaProfileType.AUDIO
		context = EncodingContext.STREAMING

		container = Codec.Container.TS
		protocol = MediaStreamProtocol.HLS

		audioCodec(Codec.Audio.AAC)
	}

	/// Direct play profiles
	// Video
	directPlayProfile {
		type = DlnaProfileType.VIDEO

		container(
			Codec.Container.ASF,
			Codec.Container.HLS,
			Codec.Container.M4V,
			Codec.Container.MKV,
			Codec.Container.MOV,
			Codec.Container.MP4,
			Codec.Container.OGM,
			Codec.Container.OGV,
			Codec.Container.TS,
			Codec.Container.VOB,
			Codec.Container.WEBM,
			Codec.Container.WMV,
			Codec.Container.XVID,
		)

		videoCodec(
			Codec.Video.AV1,
			Codec.Video.H264,
			Codec.Video.HEVC,
			Codec.Video.MPEG,
			Codec.Video.MPEG2VIDEO,
			Codec.Video.VC1,
			Codec.Video.VP8,
			Codec.Video.VP9,
		)

		audioCodec(*allowedAudioCodecs)
	}

	// Audio
	directPlayProfile {
		type = DlnaProfileType.AUDIO

		audioCodec(*allowedAudioCodecs)
	}

	/// Codec profiles
	// H264 profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.H264

		conditions {
			when {
				!supportsAVC -> ProfileConditionValue.VIDEO_PROFILE equals "none"
				else -> ProfileConditionValue.VIDEO_PROFILE inCollection listOfNotNull(
					"high",
					"main",
					"baseline",
					"constrained baseline",
					if (supportsAVCHigh10) "high 10" else null
				)
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
				ProfileConditionValue.VIDEO_PROFILE inCollection listOf(
					"high",
					"main",
					"baseline",
					"constrained baseline"
				)
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
				ProfileConditionValue.VIDEO_PROFILE equals "high 10"
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
				!supportsHevc -> ProfileConditionValue.VIDEO_PROFILE equals "none"
				else -> ProfileConditionValue.VIDEO_PROFILE inCollection listOfNotNull(
					"main",
					if (supportsHevcMain10) "main 10" else null
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
				ProfileConditionValue.VIDEO_PROFILE equals "main"
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
				ProfileConditionValue.VIDEO_PROFILE equals "main 10"
			}
		}
	}

	// AV1 profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.AV1

		conditions {
			when {
				!supportsAV1 -> ProfileConditionValue.VIDEO_PROFILE equals "none"
				!supportsAV1Main10 -> ProfileConditionValue.VIDEO_PROFILE notEquals "main 10"
				else -> ProfileConditionValue.VIDEO_PROFILE notEquals "none"
			}
		}
	}

	// VC1 profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.VC1

		conditions {
			when {
				!supportsVC1 -> ProfileConditionValue.VIDEO_PROFILE equals "none"
				else -> ProfileConditionValue.VIDEO_PROFILE notEquals "none"
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

	// TODO Use VideoRangeType enum with Jellyfin 10.11 based SDK
	val unsupportedRangeTypesAv1 = buildSet {
		if (jellyfinTenEleven) add("DOVIInvalid")

		if (!supportsAV1DolbyVision) {
			add(VideoRangeType.DOVI.serialName)
			if (!supportsAV1HDR10) add(VideoRangeType.DOVI_WITH_HDR10.serialName)
			if (jellyfinTenEleven && !supportsAV1HDR10Plus) add("DOVIWithHDR10Plus")
		}

		if (!supportsAV1HDR10Plus) {
			add(VideoRangeType.HDR10_PLUS.serialName)

			if (!mediaTest.supportsAV1HDR10()) add(VideoRangeType.HDR10.serialName)
		}
	}

	// TODO Use VideoRangeType enum with Jellyfin 10.11 based SDK
	val unsupportedRangeTypesHevc = buildSet {
		if (jellyfinTenEleven) add("DOVIInvalid")

		if (!supportsHevcDolbyVisionEL) {
			if (jellyfinTenEleven) {
				add("DOVIWithEL")
				if (!supportsHevcHDR10Plus && !KnownDefects.hevcDoviHdr10PlusBug) add("DOVIWithELHDR10Plus")
			}

			if (!supportsHevcDolbyVision) {
				add(VideoRangeType.DOVI.serialName)
				if (!supportsHevcHDR10) add(VideoRangeType.DOVI_WITH_HDR10.serialName)
				if (jellyfinTenEleven && !supportsHevcHDR10Plus && !KnownDefects.hevcDoviHdr10PlusBug) add("DOVIWithHDR10Plus")
			}
		}

		if (!supportsHevcHDR10Plus) {
			add(VideoRangeType.HDR10_PLUS.serialName)
			if (!supportsHevcHDR10) add(VideoRangeType.HDR10.serialName)
		}

		if (jellyfinTenEleven && KnownDefects.hevcDoviHdr10PlusBug) {
			add("DOVIWithHDR10Plus")
			add("DOVIWithELHDR10Plus")
		}
	}

	// Display
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
			ProfileConditionValue.VIDEO_RANGE_TYPE notEquals unsupportedRangeTypesAv1.joinToString("|")
		}

		applyConditions {
			ProfileConditionValue.VIDEO_RANGE_TYPE inCollection unsupportedRangeTypesAv1
		}
	}

	// HEVC
	if (unsupportedRangeTypesHevc.isNotEmpty()) codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.HEVC

		conditions {
			ProfileConditionValue.VIDEO_RANGE_TYPE notEquals unsupportedRangeTypesHevc.joinToString("|")
		}

		applyConditions {
			ProfileConditionValue.VIDEO_RANGE_TYPE inCollection unsupportedRangeTypesHevc
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
