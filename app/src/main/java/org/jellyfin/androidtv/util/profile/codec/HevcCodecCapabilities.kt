package org.jellyfin.androidtv.util.profile.codec

import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaFormat
import android.os.Build

class HevcCodecCapabilities(
	private val query: MediaCodecQuery,
) {
	companion object {
		// HEVC levels as reported by ffprobe are multiplied by 30, e.g. level 4.1 is 123
		internal val LEVEL_MAP: List<Pair<Int, Int>> = listOf(
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

		private const val MIME_HEVC = MediaFormat.MIMETYPE_VIDEO_HEVC
		private const val MIME_DOLBY_VISION = MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION
	}

	fun supportsHevc(): Boolean = query.hasCodecForMime(MIME_HEVC)

	fun supportsHevcMain10(): Boolean = query.hasDecoder(
		MIME_HEVC,
		CodecProfileLevel.HEVCProfileMain10,
		CodecProfileLevel.HEVCMainTierLevel4,
	)

	fun supportsHevcDolbyVision(): Boolean =
		DeviceSdk.sdkInt >= Build.VERSION_CODES.N &&
			query.hasCodecForMime(MIME_DOLBY_VISION)

	fun supportsHevcDolbyVisionEL(): Boolean =
		DeviceSdk.sdkInt >= Build.VERSION_CODES.N &&
			query.hasDecoder(
				MIME_DOLBY_VISION,
				CodecProfileLevel.DolbyVisionProfileDvheDtb,
				CodecProfileLevel.DolbyVisionLevelHd24,
			) &&
			query.supportsMultiInstance(MIME_HEVC)

	fun supportsHevcHDR10(): Boolean =
		DeviceSdk.sdkInt >= Build.VERSION_CODES.N &&
			query.hasDecoder(
				MIME_HEVC,
				CodecProfileLevel.HEVCProfileMain10HDR10,
				CodecProfileLevel.HEVCMainTierLevel4,
			)

	fun supportsHevcHDR10Plus(): Boolean =
		DeviceSdk.sdkInt >= Build.VERSION_CODES.Q &&
			query.hasDecoder(
				MIME_HEVC,
				CodecProfileLevel.HEVCProfileMain10HDR10Plus,
				CodecProfileLevel.HEVCMainTierLevel4,
			)

	fun getMainLevel(): Int = getLevel(CodecProfileLevel.HEVCProfileMain)

	fun getMain10Level(): Int = getLevel(CodecProfileLevel.HEVCProfileMain10)

	private fun getLevel(profile: Int): Int {
		val level = query.getDecoderLevel(MIME_HEVC, profile)

		return LEVEL_MAP.asReversed().find { (codecLevel, _) ->
			level >= codecLevel
		}?.second ?: 0
	}
}
