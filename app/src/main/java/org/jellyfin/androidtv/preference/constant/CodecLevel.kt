package org.jellyfin.androidtv.preference.constant

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
	LEVEL_6_2(R.string.codec_level_6_2, 62),
	LEVEL_6_1(R.string.codec_level_6_1, 61),
	LEVEL_6_0(R.string.codec_level_6_0, 60),
	LEVEL_5_2(R.string.codec_level_5_2, 52),
	LEVEL_5_1(R.string.codec_level_5_1, 51),
	LEVEL_5_0(R.string.codec_level_5_0, 50),
	LEVEL_4_2(R.string.codec_level_4_2, 42),
	LEVEL_4_1(R.string.codec_level_4_1, 41),
	LEVEL_4_0(R.string.codec_level_4_0, 40),
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
	LEVEL_6_2(R.string.codec_level_6_2, 186),
	LEVEL_6_1(R.string.codec_level_6_1, 183),
	LEVEL_6_0(R.string.codec_level_6_0, 180),
	LEVEL_5_2(R.string.codec_level_5_2, 156),
	LEVEL_5_1(R.string.codec_level_5_1, 153),
	LEVEL_5_0(R.string.codec_level_5_0, 150),
	LEVEL_4_1(R.string.codec_level_4_1, 123),
	LEVEL_4_0(R.string.codec_level_4_0, 120),
}
