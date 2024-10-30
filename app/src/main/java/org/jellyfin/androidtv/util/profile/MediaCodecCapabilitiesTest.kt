package org.jellyfin.androidtv.util.profile

import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Size
import timber.log.Timber

class MediaCodecCapabilitiesTest {
	private val mediaCodecList by lazy { MediaCodecList(MediaCodecList.REGULAR_CODECS) }

	// AVC levels as reported by ffprobe are multiplied by 10, e.g. level 4.1 is 41. Level 1b is set to 9
	private val avcLevelStrings = listOf(
		CodecProfileLevel.AVCLevel1b to "9",
		CodecProfileLevel.AVCLevel1 to "10",
		CodecProfileLevel.AVCLevel11 to "11",
		CodecProfileLevel.AVCLevel12 to "12",
		CodecProfileLevel.AVCLevel13 to "13",
		CodecProfileLevel.AVCLevel2 to "20",
		CodecProfileLevel.AVCLevel21 to "21",
		CodecProfileLevel.AVCLevel22 to "22",
		CodecProfileLevel.AVCLevel3 to "30",
		CodecProfileLevel.AVCLevel31 to "31",
		CodecProfileLevel.AVCLevel32 to "32",
		CodecProfileLevel.AVCLevel4 to "40",
		CodecProfileLevel.AVCLevel41 to "41",
		CodecProfileLevel.AVCLevel42 to "42",
		CodecProfileLevel.AVCLevel5 to "50",
		CodecProfileLevel.AVCLevel51 to "51",
		CodecProfileLevel.AVCLevel52 to "52",
	)

	// HEVC levels as reported by ffprobe are multiplied by 30, e.g. level 4.1 is 123
	private val hevcLevelStrings = listOf(
		CodecProfileLevel.HEVCMainTierLevel1 to "30",
		CodecProfileLevel.HEVCMainTierLevel2 to "60",
		CodecProfileLevel.HEVCMainTierLevel21 to "63",
		CodecProfileLevel.HEVCMainTierLevel3 to "90",
		CodecProfileLevel.HEVCMainTierLevel31 to "93",
		CodecProfileLevel.HEVCMainTierLevel4 to "120",
		CodecProfileLevel.HEVCMainTierLevel41 to "123",
		CodecProfileLevel.HEVCMainTierLevel5 to "150",
		CodecProfileLevel.HEVCMainTierLevel51 to "153",
		CodecProfileLevel.HEVCMainTierLevel52 to "156",
		CodecProfileLevel.HEVCMainTierLevel6 to "180",
		CodecProfileLevel.HEVCMainTierLevel61 to "183",
		CodecProfileLevel.HEVCMainTierLevel62 to "186",
	)

	fun supportsAV1(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
		hasCodecForMime(MediaFormat.MIMETYPE_VIDEO_AV1)

	fun supportsAV1Main10(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
		hasDecoder(
			MediaFormat.MIMETYPE_VIDEO_AV1,
			CodecProfileLevel.AV1ProfileMain10,
			CodecProfileLevel.AV1Level5
		)

	fun supportsAVC(): Boolean = hasCodecForMime(MediaFormat.MIMETYPE_VIDEO_AVC)

	fun supportsAVCHigh10(): Boolean = hasDecoder(
		MediaFormat.MIMETYPE_VIDEO_AVC,
		CodecProfileLevel.AVCProfileHigh10,
		CodecProfileLevel.AVCLevel4
	)

	fun getAVCMainLevel(): String = getAVCLevelString(
		CodecProfileLevel.AVCProfileMain
	)

	fun getAVCHigh10Level(): String = getAVCLevelString(
		CodecProfileLevel.AVCProfileHigh10
	)

	private fun getAVCLevelString(profile: Int): String {
		val level = getDecoderLevel(MediaFormat.MIMETYPE_VIDEO_AVC, profile)

		return avcLevelStrings.asReversed().find { item: Pair<Int, String> ->
			level >= item.first
		}?.second ?: "0"
	}

	fun supportsHevc(): Boolean = hasCodecForMime(MediaFormat.MIMETYPE_VIDEO_HEVC)

	fun supportsHevcMain10(): Boolean = hasDecoder(
		MediaFormat.MIMETYPE_VIDEO_HEVC,
		CodecProfileLevel.HEVCProfileMain10,
		CodecProfileLevel.HEVCMainTierLevel4
	)

	fun getHevcMainLevel(): String = getHevcLevelString(
		CodecProfileLevel.HEVCProfileMain
	)

	fun getHevcMain10Level(): String = getHevcLevelString(
		CodecProfileLevel.HEVCProfileMain10
	)

	private fun getHevcLevelString(profile: Int): String {
		val level = getDecoderLevel(MediaFormat.MIMETYPE_VIDEO_HEVC, profile)

		return hevcLevelStrings.asReversed().find { item: Pair<Int, String> ->
			level >= item.first
		}?.second ?: "0"
	}

	private fun getDecoderLevel(mime: String, profile: Int): Int {
		var maxLevel = 0

		for (info in mediaCodecList.codecInfos) {
			if (info.isEncoder) continue

			try {
				val capabilities = info.getCapabilitiesForType(mime)
				for (profileLevel in capabilities.profileLevels) {
					if (profileLevel.profile == profile) {
						maxLevel = maxOf(maxLevel, profileLevel.level)
					}
				}
			} catch (_: IllegalArgumentException) {
				// Decoder not supported - ignore
			}
		}

		return maxLevel
	}

	private fun hasDecoder(mime: String, profile: Int, level: Int): Boolean {
		for (info in mediaCodecList.codecInfos) {
			if (info.isEncoder) continue

			try {
				val capabilities = info.getCapabilitiesForType(mime)
				for (profileLevel in capabilities.profileLevels) {
					if (profileLevel.profile != profile) continue

					// H.263 levels are not completely ordered:
					// Level45 support only implies Level10 support
					if (mime.equals(MediaFormat.MIMETYPE_VIDEO_H263, ignoreCase = true)) {
						if (profileLevel.level != level && profileLevel.level == CodecProfileLevel.H263Level45 && level > CodecProfileLevel.H263Level10) {
							continue
						}
					}

					if (profileLevel.level >= level) return true
				}
			} catch (_: IllegalArgumentException) {
				// Decoder not supported - ignore
			}
		}

		return false
	}

	private fun hasCodecForMime(mime: String): Boolean {
		for (info in mediaCodecList.codecInfos) {
			if (info.isEncoder) continue

			if (info.supportedTypes.any { it.equals(mime, ignoreCase = true) }) {
				Timber.i("found codec %s for mime %s", info.name, mime)
				return true
			}
		}

		return false
	}

	fun getMaxResolution(mime: String): Size {
		var maxWidth = 0
		var maxHeight = 0

		for (info in mediaCodecList.codecInfos) {
			if (info.isEncoder) continue

			try {
				val capabilities = info.getCapabilitiesForType(mime)
				val videoCapabilities = capabilities.videoCapabilities ?: continue
				val supportedWidth = videoCapabilities.supportedWidths?.upper ?: continue
				val supportedHeight = videoCapabilities.supportedHeights?.upper ?: continue

				maxWidth = maxOf(maxWidth, supportedWidth)
				maxHeight = maxOf(maxHeight, supportedHeight)

			} catch (_: IllegalArgumentException) {
				// Decoder not supported - ignore
			}
		}

		return Size(maxWidth, maxHeight)
	}

}
