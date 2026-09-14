package org.jellyfin.androidtv.util.profile

import android.content.Context
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Format
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioCapabilities
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.jellyfin.androidtv.preference.constant.HdrFormat
import org.jellyfin.androidtv.preference.constant.HdrOverrideMode
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
import timber.log.Timber
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

private val hlsMpegTsAudioCodecs = arrayOf(
	Codec.Audio.AAC,
	Codec.Audio.AC3,
	Codec.Audio.EAC3,
	Codec.Audio.MP3
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
	Codec.Audio.TRUEHD
)

/** What this device can hand over untouched to whatever decodes its audio. */
data class PassthroughSupport(
	/** Codec names that reach that decoder as a bitstream. */
	val codecs: Set<String>,
	/** Whether a lossless format gets through, which the rule below reads as an amplifier. */
	val lossless: Boolean,
)

/** Surround codecs that only arrive intact as a bitstream. */
private val surroundCodecs = setOf(
	Codec.Audio.AC3,
	Codec.Audio.EAC3,
	Codec.Audio.DTS,
	Codec.Audio.DCA,
	Codec.Audio.MLP,
	Codec.Audio.TRUEHD,
)

/**
 * Asks the platform which surround formats it can pass through, at 5.1 and 48 kHz.
 *
 * From Android 13 one call describes the currently routed output, which media3 uses. Before that
 * the platform answers for every output the device has rather than the one in use, which is the
 * best available there.
 */
@OptIn(UnstableApi::class)
fun getPassthroughSupport(context: Context): PassthroughSupport {
	val attributes = AudioAttributes.Builder()
		.setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
		.setUsage(C.USAGE_MEDIA)
		.build()
	@Suppress("DEPRECATION")
	val capabilities = AudioCapabilities.getCapabilities(context, attributes, null)

	fun passesThrough(mimeType: String): Boolean {
		val format = Format.Builder()
			.setSampleMimeType(mimeType)
			.setSampleRate(48000)
			.setChannelCount(6)
			.build()
		val output = capabilities.getEncodingAndChannelConfigForPassthrough(format, attributes)
		// media3 falls back from DTS-HD to its DTS core, which is not DTS-HD support.
		return output != null && output.first == MimeTypes.getEncoding(mimeType, null)
	}

	val trueHd = passesThrough(MimeTypes.AUDIO_TRUEHD)
	val dts = passesThrough(MimeTypes.AUDIO_DTS)
	val codecs = buildSet {
		if (passesThrough(MimeTypes.AUDIO_AC3)) add(Codec.Audio.AC3)
		if (passesThrough(MimeTypes.AUDIO_E_AC3)) add(Codec.Audio.EAC3)
		if (dts) addAll(listOf(Codec.Audio.DTS, Codec.Audio.DCA))
		if (trueHd) addAll(listOf(Codec.Audio.TRUEHD, Codec.Audio.MLP))
	}
	return PassthroughSupport(codecs, lossless = trueHd || passesThrough(MimeTypes.AUDIO_DTS_HD))
		.also { Timber.i("Audio passthrough: %s", it) }
}

/**
 * Returns whether to offer only what this device can bitstream.
 *
 * The profile sees the client, not the chain behind it. Passthrough says which bitstreams the
 * current output accepts and nothing about what a television further along does to multichannel
 * PCM: a Chromecast with Google TV reports a direct 5.1 PCM profile on a chain that delivers
 * stereo. So a bitstream is the safer offer whenever the device can make one.
 *
 * Lossless is left alone on the assumption that it means an amplifier, which takes PCM as well.
 * That assumption is why this belongs in a user setting, with this rule as its automatic default.
 *
 * Only AC-3 and E-AC-3 count as a fallback; ffmpeg's DTS encoder is experimental.
 */
private fun PassthroughSupport.shouldLimit(allowed: Array<String>) = !lossless &&
	(Codec.Audio.AC3 in codecs && Codec.Audio.AC3 in allowed ||
		Codec.Audio.EAC3 in codecs && Codec.Audio.EAC3 in allowed)

/**
 * Encoder preferences for a transcoding profile. Source track copying is selected separately by
 * the server.
 *
 * The head of this list is what the server encodes to, so a codec that arrives intact goes first.
 * AC-3 before E-AC-3: ffmpeg's E-AC-3 encoder is less good than its AC-3 one.
 */
private fun Array<String>.forTranscoding(allowed: Array<String>, passthrough: Set<String>) =
	filter { it in allowed }
		.sortedBy { when {
			it == Codec.Audio.AC3 && it in passthrough -> 0
			it == Codec.Audio.EAC3 && it in passthrough -> 1
			it == Codec.Audio.AAC -> 2
			else -> 3
		} }.toTypedArray()

private fun UserPreferences.getMaxBitrate(): Int {
	var maxBitrate = this[UserPreferences.maxBitrate].toFloatOrNull()

	// The value "0" was used in an older release, make sure we prevent that from being used to avoid video not playing
	if (maxBitrate == null || maxBitrate < 0.01f) maxBitrate = UserPreferences.maxBitrate.defaultValue.toFloat()

	// Convert megabit to bit
	return (maxBitrate * 1_000_000).roundToInt()
}

private fun UserPreferences.getHdrRangeTypesFor(mode: HdrOverrideMode): Set<VideoRangeType> =
	HdrFormat.entries
		.filter { this[it.preference] == mode }
		.flatMapTo(mutableSetOf()) { it.videoRangeTypes }

fun createDeviceProfile(
	context: Context,
	userPreferences: UserPreferences,
	serverVersion: ServerVersion,
) = createDeviceProfile(
	mediaTest = MediaCodecCapabilitiesTest(userPreferences[UserPreferences.softwareCodecsEnabled]),
	passthroughSupport = getPassthroughSupport(context),
	maxBitrate = userPreferences.getMaxBitrate(),
	isAC3Enabled = userPreferences[UserPreferences.ac3Enabled],
	downMixAudio = userPreferences[UserPreferences.audioBehaviour] == AudioBehavior.DOWNMIX_TO_STEREO,
	assDirectPlay = userPreferences[UserPreferences.assDirectPlay],
	pgsDirectPlay = userPreferences[UserPreferences.pgsDirectPlay],
	userAVCLevel = userPreferences[UserPreferences.userAVCLevel].level,
	userHEVCLevel = userPreferences[UserPreferences.userHEVCLevel].level,
	forceEnabledHdr = userPreferences.getHdrRangeTypesFor(HdrOverrideMode.ENABLE),
	forceDisabledHdr = userPreferences.getHdrRangeTypesFor(HdrOverrideMode.DISABLE),
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
	forceEnabledHdr: Set<VideoRangeType>,
	forceDisabledHdr: Set<VideoRangeType>,
	passthroughSupport: PassthroughSupport = PassthroughSupport(emptySet(), lossless = false),
) = buildDeviceProfile {
	val candidateAudioCodecs = when {
		downMixAudio -> downmixSupportedAudioCodecs
		!isAC3Enabled -> supportedAudioCodecs.filterNot { it == Codec.Audio.EAC3 || it == Codec.Audio.AC3 }.toTypedArray()
		else -> supportedAudioCodecs
	}
	val limitToPassthrough = !downMixAudio && passthroughSupport.shouldLimit(candidateAudioCodecs)
	val allowedAudioCodecs = candidateAudioCodecs
		.filterNot { limitToPassthrough && it in surroundCodecs && it !in passthroughSupport.codecs }
		.toTypedArray()

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
	val hlsVideoCodecs = listOfNotNull(
		if (supportsHevc) Codec.Video.HEVC else null,
		Codec.Video.H264
	).toTypedArray()

	transcodingProfile {
		type = DlnaProfileType.VIDEO
		context = EncodingContext.STREAMING

		container = Codec.Container.TS
		protocol = MediaStreamProtocol.HLS

		videoCodec(*hlsVideoCodecs)
		audioCodec(*hlsMpegTsAudioCodecs.forTranscoding(allowedAudioCodecs, passthroughSupport.codecs))

		copyTimestamps = false
		enableSubtitlesInManifest = true
	}

	transcodingProfile {
		type = DlnaProfileType.VIDEO
		context = EncodingContext.STREAMING

		container = Codec.Container.MP4
		protocol = MediaStreamProtocol.HLS

		videoCodec(*hlsVideoCodecs)
		audioCodec(*hlsFmp4AudioCodecs.forTranscoding(allowedAudioCodecs, passthroughSupport.codecs))

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

	val unsupportedRangeTypesAv1 = buildSet {
		add(VideoRangeType.DOVI_INVALID)

		if (!supportsAV1DolbyVision) {
			add(VideoRangeType.DOVI)
			if (!supportsAV1HDR10) add(VideoRangeType.DOVI_WITH_HDR10)
			if (!supportsAV1HDR10Plus) add(VideoRangeType.DOVI_WITH_HDR10_PLUS)
		}

		if (!supportsAV1HDR10Plus) {
			add(VideoRangeType.HDR10_PLUS)

			if (!mediaTest.supportsAV1HDR10()) add(VideoRangeType.HDR10)
		}
	} - forceEnabledHdr + forceDisabledHdr

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
	} - forceEnabledHdr + forceDisabledHdr

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
