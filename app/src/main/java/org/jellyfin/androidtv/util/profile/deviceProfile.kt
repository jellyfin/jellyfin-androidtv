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
import org.jellyfin.sdk.model.deviceprofile.ProfileConditionsBuilder
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

private val avcRefFrameLimits = arrayOf(
	12 to 1200,
	4 to 1900,
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
	val hevcDoviHdr10PlusBug = KnownDefects.hevcDoviHdr10PlusBug

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

	hlsVideoTranscodingProfile(
		segmentContainer = Codec.Container.TS,
		videoCodecs = hlsVideoCodecs,
		audioCodecs = hlsMpegTsAudioCodecs.filter(allowedAudioCodecs::contains).toTypedArray(),
	)

	hlsVideoTranscodingProfile(
		segmentContainer = Codec.Container.MP4,
		videoCodecs = hlsFmp4VideoCodecs,
		audioCodecs = hlsFmp4AudioCodecs.filter(allowedAudioCodecs::contains).toTypedArray(),
	)

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

	/// Codec profiles
	// H264 profile
	videoCodecProfile(Codec.Video.H264) {
		when {
			!supportsAVC -> ProfileConditionValue.VIDEO_PROFILE equals PROFILE_NONE
			else -> ProfileConditionValue.VIDEO_PROFILE inCollection
				avcBaseProfiles + listOfNotNull(if (supportsAVCHigh10) AVC_PROFILE_HIGH_10 else null)
		}
	}
	if (supportsAVC) videoCodecProfile(
		Codec.Video.H264,
		applyWhen = { ProfileConditionValue.VIDEO_PROFILE inCollection avcBaseProfiles },
	) {
		ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals avcMainLevel
	}
	if (supportsAVCHigh10) videoCodecProfile(
		Codec.Video.H264,
		applyWhen = { ProfileConditionValue.VIDEO_PROFILE equals AVC_PROFILE_HIGH_10 },
	) {
		ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals avcHigh10Level
	}

	// H264 ref frames profiles
	for ((refFrames, minWidth) in avcRefFrameLimits) videoCodecProfile(
		Codec.Video.H264,
		applyWhen = { ProfileConditionValue.WIDTH greaterThanOrEquals minWidth },
	) {
		ProfileConditionValue.REF_FRAMES lowerThanOrEquals refFrames
	}

	// HEVC profiles
	videoCodecProfile(Codec.Video.HEVC) {
		when {
			!supportsHevc -> ProfileConditionValue.VIDEO_PROFILE equals PROFILE_NONE
			else -> ProfileConditionValue.VIDEO_PROFILE inCollection listOfNotNull(
				PROFILE_MAIN,
				if (supportsHevcMain10) PROFILE_MAIN_10 else null
			)
		}
	}
	if (supportsHevc) videoCodecProfile(
		Codec.Video.HEVC,
		applyWhen = { ProfileConditionValue.VIDEO_PROFILE equals PROFILE_MAIN },
	) {
		ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals hevcMainLevel
	}
	if (supportsHevcMain10) videoCodecProfile(
		Codec.Video.HEVC,
		applyWhen = { ProfileConditionValue.VIDEO_PROFILE equals PROFILE_MAIN_10 },
	) {
		ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals hevcMain10Level
	}

	// AV1 profile
	videoCodecProfile(Codec.Video.AV1) {
		when {
			!supportsAV1 -> ProfileConditionValue.VIDEO_PROFILE equals PROFILE_NONE
			!supportsAV1Main10 -> ProfileConditionValue.VIDEO_PROFILE notEquals PROFILE_MAIN_10
			else -> ProfileConditionValue.VIDEO_PROFILE notEquals PROFILE_NONE
		}
	}

	// Codecs without profile/level conditions, disabled entirely when the device has no decoder for them
	val gatedVideoCodecs = listOf(
		Codec.Video.VC1 to supportsVC1,
		Codec.Video.MPEG1VIDEO to supportsMpeg2,
		Codec.Video.MPEG2VIDEO to supportsMpeg2,
		Codec.Video.MPEG4 to supportsMpeg4Asp,
		Codec.Video.VP8 to supportsVP8,
		Codec.Video.VP9 to supportsVP9,
	)

	for ((codec, supported) in gatedVideoCodecs) videoCodecProfile(codec) {
		when {
			!supported -> ProfileConditionValue.VIDEO_PROFILE equals PROFILE_NONE
			else -> ProfileConditionValue.VIDEO_PROFILE notEquals PROFILE_NONE
		}
	}

	// Get max resolutions for common codecs
	val maxResolutions = listOf(
		Codec.Video.H264 to mediaTest.getMaxResolution(MimeTypes.VIDEO_H264),
		Codec.Video.HEVC to mediaTest.getMaxResolution(MimeTypes.VIDEO_H265),
		Codec.Video.AV1 to mediaTest.getMaxResolution(MimeTypes.VIDEO_AV1),
		Codec.Video.VC1 to mediaTest.getMaxResolution(MimeTypes.VIDEO_VC1),
	)

	for ((codec, maxResolution) in maxResolutions) videoCodecProfile(codec) {
		ProfileConditionValue.WIDTH lowerThanOrEquals maxResolution.width
		ProfileConditionValue.HEIGHT lowerThanOrEquals maxResolution.height
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
			add(VideoRangeType.DOVI_WITH_EL)
			if (!supportsHevcHDR10Plus && !hevcDoviHdr10PlusBug) add(VideoRangeType.DOVI_WITH_ELHDR10_PLUS)

			if (!supportsHevcDolbyVision) {
				add(VideoRangeType.DOVI)
				if (!supportsHevcHDR10) add(VideoRangeType.DOVI_WITH_HDR10)
				if (!supportsHevcHDR10Plus && !hevcDoviHdr10PlusBug) add(VideoRangeType.DOVI_WITH_HDR10_PLUS)
			}
		}

		if (!supportsHevcHDR10Plus) {
			add(VideoRangeType.HDR10_PLUS)
			if (!supportsHevcHDR10) add(VideoRangeType.HDR10)
		}

		if (hevcDoviHdr10PlusBug) {
			add(VideoRangeType.DOVI_WITH_HDR10_PLUS)
			add(VideoRangeType.DOVI_WITH_ELHDR10_PLUS)
		}
	}

	// Note: The codec profiles use a workaround to create correct behavior
	// The notEquals condition will always fail the ConditionProcessor test in the server so we use applyConditions to only have the codec
	// profile be active when the media in question uses one of the unsupported range types. The server will then use the value of the
	// notEquals in the StreamBuilder to create a correct transcode pipeline
	val unsupportedRangeTypes = listOf(
		Codec.Video.AV1 to unsupportedRangeTypesAv1,
		Codec.Video.HEVC to unsupportedRangeTypesHevc,
	)

	for ((codec, rangeTypes) in unsupportedRangeTypes) {
		if (rangeTypes.isEmpty()) continue

		videoCodecProfile(
			codec,
			applyWhen = {
				ProfileConditionValue.VIDEO_RANGE_TYPE inCollection rangeTypes.map { it.serialName }
			},
		) {
			ProfileConditionValue.VIDEO_RANGE_TYPE notEquals rangeTypes.joinToString("|") { it.serialName }
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

// Little helper function to define the two near-identical HLS video transcoding profiles
private fun DeviceProfileBuilder.hlsVideoTranscodingProfile(
	segmentContainer: String,
	videoCodecs: Array<String>,
	audioCodecs: Array<String>,
) = transcodingProfile {
	type = DlnaProfileType.VIDEO
	context = EncodingContext.STREAMING

	container = segmentContainer
	protocol = MediaStreamProtocol.HLS

	videoCodec(*videoCodecs)
	audioCodec(*audioCodecs)

	copyTimestamps = false
	enableSubtitlesInManifest = true
}

// Little helper function to more easily define video codec profiles
private fun DeviceProfileBuilder.videoCodecProfile(
	videoCodec: String,
	applyWhen: (ProfileConditionsBuilder.() -> Unit)? = null,
	conditions: ProfileConditionsBuilder.() -> Unit,
) = codecProfile {
	type = CodecType.VIDEO
	codec = videoCodec

	this.conditions(conditions)
	if (applyWhen != null) applyConditions(applyWhen)
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
