package org.jellyfin.androidtv.util.profile

import android.media.MediaFormat
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.sdk.model.api.CodecType
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.deviceprofile.DeviceProfileBuilder
import org.jellyfin.sdk.model.deviceprofile.buildDeviceProfile

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

fun createDeviceProfile(
	maxBitrate: Int,
	disableDirectPlay: Boolean,
	isAC3Enabled: Boolean,
	downMixAudio: Boolean
) = buildDeviceProfile {
	val allowedAudioCodecs = when {
		downMixAudio -> downmixSupportedAudioCodecs
		!isAC3Enabled -> supportedAudioCodecs.filterNot { it == Codec.Audio.EAC3 || it == Codec.Audio.AC3 }.toTypedArray()
		else -> supportedAudioCodecs
	}

	val mediaTest = MediaCodecCapabilitiesTest()
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
	val maxResolution = mediaTest.getMaxResolution(MediaFormat.MIMETYPE_VIDEO_AVC)

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

		container = Codec.Container.MP3

		audioCodec(Codec.Audio.MP3)
	}

	/// Direct play profiles
	if (!disableDirectPlay) {
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
					if (supportsAVCHigh10) "main 10" else null
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

	// Limit video resolution support for older devices
	codecProfile {
		type = CodecType.VIDEO

		conditions {
			ProfileConditionValue.WIDTH lowerThanOrEquals maxResolution.width
			ProfileConditionValue.HEIGHT lowerThanOrEquals maxResolution.height
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
	subtitleProfile(Codec.Subtitle.IDX, embedded = true, encode = true)
	subtitleProfile(Codec.Subtitle.PGS, embedded = true, encode = true)
	subtitleProfile(Codec.Subtitle.PGSSUB, embedded = true, encode = true)

	// ASS/SSA renderer only supports a small subset of the specification so encoding is required for correct rendering
	subtitleProfile(Codec.Subtitle.ASS, encode = true)
	subtitleProfile(Codec.Subtitle.SSA, encode = true)

	// Unsupported formats that need encoding
	subtitleProfile(Codec.Subtitle.DVDSUB, encode = true)
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
