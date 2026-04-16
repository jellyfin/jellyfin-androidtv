package org.jellyfin.androidtv.preference.constant

import android.media.MediaCodecInfo.CodecProfileLevel
import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

/**
 * User-defined AVC (H.264) codec level override.
 * Values correspond to MediaCodecInfo.CodecProfileLevel constants.
 */
enum class AVCLevel(
	override val nameRes: Int,
	val level: Int,
) : PreferenceEnum {
	AUTO(R.string.auto, -1),
	LEVEL_6_2(R.string.codec_level_6_2, CodecProfileLevel.AVCLevel62),
	LEVEL_6_1(R.string.codec_level_6_1, CodecProfileLevel.AVCLevel61),
	LEVEL_6_0(R.string.codec_level_6_0, CodecProfileLevel.AVCLevel6),
	LEVEL_5_2(R.string.codec_level_5_2, CodecProfileLevel.AVCLevel52),
	LEVEL_5_1(R.string.codec_level_5_1, CodecProfileLevel.AVCLevel51),
	LEVEL_5_0(R.string.codec_level_5_0, CodecProfileLevel.AVCLevel5),
	LEVEL_4_2(R.string.codec_level_4_2, CodecProfileLevel.AVCLevel42),
	LEVEL_4_1(R.string.codec_level_4_1, CodecProfileLevel.AVCLevel41),
	LEVEL_4_0(R.string.codec_level_4_0, CodecProfileLevel.AVCLevel4),
}

/**
 * User-defined HEVC (H.265) codec level override.
 * Values correspond to MediaCodecInfo.CodecProfileLevel constants.
 */
enum class HEVCLevel(
	override val nameRes: Int,
	val level: Int,
) : PreferenceEnum {
	AUTO(R.string.auto, -1),
	LEVEL_6_2(R.string.codec_level_6_2, CodecProfileLevel.HEVCMainTierLevel62),
	LEVEL_6_1(R.string.codec_level_6_1, CodecProfileLevel.HEVCMainTierLevel61),
	LEVEL_6_0(R.string.codec_level_6_0, CodecProfileLevel.HEVCMainTierLevel6),
	LEVEL_5_2(R.string.codec_level_5_2, CodecProfileLevel.HEVCMainTierLevel52),
	LEVEL_5_1(R.string.codec_level_5_1, CodecProfileLevel.HEVCMainTierLevel51),
	LEVEL_5_0(R.string.codec_level_5_0, CodecProfileLevel.HEVCMainTierLevel5),
	LEVEL_4_1(R.string.codec_level_4_1, CodecProfileLevel.HEVCMainTierLevel41),
	LEVEL_4_0(R.string.codec_level_4_0, CodecProfileLevel.HEVCMainTierLevel4),
}
