package org.jellyfin.androidtv.util.profile

import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import timber.log.Timber

class MediaCodecCapabilitiesTest {
	private val mediaCodecList by lazy { MediaCodecList(MediaCodecList.REGULAR_CODECS) }

	fun supportsAV1(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
		hasCodecForMime(MediaFormat.MIMETYPE_VIDEO_AV1)

	fun supportsAV1Main10(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
		hasDecoder(
			MediaFormat.MIMETYPE_VIDEO_AV1,
			CodecProfileLevel.AV1ProfileMain10,
			CodecProfileLevel.AV1Level5
		)

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

	fun supportsAVCHigh10(): Boolean = hasDecoder(
		MediaFormat.MIMETYPE_VIDEO_AVC,
		CodecProfileLevel.AVCProfileHigh10,
		CodecProfileLevel.AVCLevel4
	)

	private fun getHevcLevelString(profile: Int) : String {
		val level = getDecoderLevel(MediaFormat.MIMETYPE_VIDEO_HEVC, profile)

		when {
			level < CodecProfileLevel.HEVCMainTierLevel1 -> return "0"
			level < CodecProfileLevel.HEVCMainTierLevel2 -> return "30"
			level < CodecProfileLevel.HEVCMainTierLevel21 -> return "60"
			level < CodecProfileLevel.HEVCMainTierLevel3 -> return "63"
			level < CodecProfileLevel.HEVCMainTierLevel31 -> return "90"
			level < CodecProfileLevel.HEVCMainTierLevel4 -> return "93"
			level < CodecProfileLevel.HEVCMainTierLevel41 -> return "120"
			level < CodecProfileLevel.HEVCMainTierLevel5 -> return "123"
			level < CodecProfileLevel.HEVCMainTierLevel51 -> return "150"
			level < CodecProfileLevel.HEVCMainTierLevel52 -> return "153"
			level < CodecProfileLevel.HEVCMainTierLevel6 -> return "156"
			level < CodecProfileLevel.HEVCMainTierLevel61 -> return "180"
			level < CodecProfileLevel.HEVCMainTierLevel62 -> return "183"
			else -> return "186"
		}
	}

	private fun getDecoderLevel(mime: String, profile: Int) : Int {
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
			} catch (e: IllegalArgumentException) {
				Timber.w(e)
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
			} catch (e: IllegalArgumentException) {
				Timber.w(e)
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
}
