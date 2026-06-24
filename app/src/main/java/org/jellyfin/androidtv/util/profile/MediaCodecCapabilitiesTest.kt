package org.jellyfin.androidtv.util.profile

import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Size
import androidx.media3.common.MimeTypes
import org.jellyfin.androidtv.util.profile.codec.AvcCodecCapabilities
import org.jellyfin.androidtv.util.profile.codec.MediaCodecQuery

class MediaCodecCapabilitiesTest(
	private val softwareCodecsEnabled: Boolean,
) {
	private val mediaCodecList by lazy { MediaCodecList(MediaCodecList.REGULAR_CODECS) }
	private val codecQuery by lazy { MediaCodecQuery(mediaCodecList, softwareCodecsEnabled) }
	private val avc by lazy { AvcCodecCapabilities(codecQuery) }

	// Map common Dolby Vision Profiles to their corresponding CodecProfileLevel constant
	private object DolbyVisionProfiles {
		val Profile5: Int by lazy {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
				CodecProfileLevel.DolbyVisionProfileDvheStn else -1
		}
		val Profile7: Int by lazy {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
				CodecProfileLevel.DolbyVisionProfileDvheDtb else -1
		}
		val Profile8: Int by lazy {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
				CodecProfileLevel.DolbyVisionProfileDvheSt else -1
		}
	}

	// Some devices (e.g., Fire OS) may support AV1 below the official API level
	// Use the platform constant if the API level is met; otherwise fall back to the literal value
	// Reference:
	// https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/media/java/android/media/MediaCodecInfo.java
	private object AV1ProfileLevel {
		val ProfileMain10: Int by lazy {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
				CodecProfileLevel.AV1ProfileMain10 else 0x2
		}
		val ProfileMain10HDR10: Int by lazy {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
				CodecProfileLevel.AV1ProfileMain10HDR10 else 0x1000
		}
		val ProfileMain10HDR10Plus: Int by lazy {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
				CodecProfileLevel.AV1ProfileMain10HDR10Plus else 0x2000
		}
		val DolbyVisionProfile10: Int by lazy {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
				CodecProfileLevel.DolbyVisionProfileDvav110 else 0x400
		}
		val Level5: Int by lazy {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
				CodecProfileLevel.AV1Level5 else 0x1000
		}
	}

	// HEVC levels as reported by ffprobe are multiplied by 30, e.g. level 4.1 is 123
	private val hevcLevels = listOf(
		CodecProfileLevel.HEVCMainTierLevel1 to 30,
		CodecProfileLevel.HEVCMainTierLevel2 to 60,
		CodecProfileLevel.HEVCMainTierLevel21 to 63,
		CodecProfileLevel.HEVCMainTierLevel3 to 90,
		CodecProfileLevel.HEVCMainTierLevel31 to 93,
		CodecProfileLevel.HEVCMainTierLevel4 to 120,
		CodecProfileLevel.HEVCMainTierLevel41 to 123,
		CodecProfileLevel.HEVCMainTierLevel5 to 150,
		CodecProfileLevel.HEVCMainTierLevel51 to 153,
		CodecProfileLevel.HEVCMainTierLevel52 to 156,
		CodecProfileLevel.HEVCMainTierLevel6 to 180,
		CodecProfileLevel.HEVCMainTierLevel61 to 183,
		CodecProfileLevel.HEVCMainTierLevel62 to 186,
	)

	fun supportsAV1(): Boolean = codecQuery.hasCodecForMime(MimeTypes.VIDEO_AV1)

	fun supportsAV1Main10(): Boolean = codecQuery.hasDecoder(
		MimeTypes.VIDEO_AV1,
		AV1ProfileLevel.ProfileMain10,
		AV1ProfileLevel.Level5
	)

	fun supportsAV1DolbyVision(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
		codecQuery.hasDecoder(
			MimeTypes.VIDEO_DOLBY_VISION,
			AV1ProfileLevel.DolbyVisionProfile10,
			CodecProfileLevel.DolbyVisionLevelHd24
		)

	fun supportsAV1HDR10(): Boolean = codecQuery.hasDecoder(
		MimeTypes.VIDEO_AV1,
		AV1ProfileLevel.ProfileMain10HDR10,
		AV1ProfileLevel.Level5
	)

	fun supportsAV1HDR10Plus(): Boolean = codecQuery.hasDecoder(
		MimeTypes.VIDEO_AV1,
		AV1ProfileLevel.ProfileMain10HDR10Plus,
		AV1ProfileLevel.Level5
	)

	fun supportsAC4(): Boolean = codecQuery.hasCodecForMime(MimeTypes.AUDIO_AC4)

	fun supportsAVC(): Boolean = avc.supportsAvc()

	fun supportsAVCHigh10(): Boolean = avc.supportsAvcHigh10()

	fun getAVCMainLevel(): Int = avc.getMainLevel()

	fun getAVCHigh10Level(): Int = avc.getHigh10Level()

	fun supportsHevc(): Boolean = codecQuery.hasCodecForMime(MediaFormat.MIMETYPE_VIDEO_HEVC)

	fun supportsHevcMain10(): Boolean = codecQuery.hasDecoder(
		MediaFormat.MIMETYPE_VIDEO_HEVC,
		CodecProfileLevel.HEVCProfileMain10,
		CodecProfileLevel.HEVCMainTierLevel4
	)

	// Can safely assume Dolby Vision decoders support single-layer HEVC profiles
	fun supportsHevcDolbyVision(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
		codecQuery.hasCodecForMime(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION)

	// Checks for Dolby Vision Profile 7 (Enhancement Layer) and multi-instance HEVC support
	fun supportsHevcDolbyVisionEL(): Boolean =
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
			codecQuery.hasDecoder(
				MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION,
				DolbyVisionProfiles.Profile7,
				CodecProfileLevel.DolbyVisionLevelHd24
			) &&
			codecQuery.supportsMultiInstance(MediaFormat.MIMETYPE_VIDEO_HEVC)

	fun supportsHevcHDR10(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
		codecQuery.hasDecoder(
			MediaFormat.MIMETYPE_VIDEO_HEVC,
			CodecProfileLevel.HEVCProfileMain10HDR10,
			CodecProfileLevel.HEVCMainTierLevel4
		)

	fun supportsHevcHDR10Plus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
		codecQuery.hasDecoder(
			MediaFormat.MIMETYPE_VIDEO_HEVC,
			CodecProfileLevel.HEVCProfileMain10HDR10Plus,
			CodecProfileLevel.HEVCMainTierLevel4
		)

	fun getHevcMainLevel(): Int = getHevcLevel(
		CodecProfileLevel.HEVCProfileMain
	)

	fun getHevcMain10Level(): Int = getHevcLevel(
		CodecProfileLevel.HEVCProfileMain10
	)

	private fun getHevcLevel(profile: Int): Int {
		val level = codecQuery.getDecoderLevel(MediaFormat.MIMETYPE_VIDEO_HEVC, profile)

		return hevcLevels.asReversed().find { item ->
			level >= item.first
		}?.second ?: 0
	}

	fun supportsVc1(): Boolean = codecQuery.hasCodecForMime(MimeTypes.VIDEO_VC1)

	fun getMaxResolution(mime: String): Size = codecQuery.getMaxResolution(mime)
}
