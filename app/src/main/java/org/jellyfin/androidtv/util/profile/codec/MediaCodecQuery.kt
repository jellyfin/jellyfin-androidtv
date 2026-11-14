package org.jellyfin.androidtv.util.profile.codec

import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Size
import org.jellyfin.androidtv.util.AndroidVersion
import timber.log.Timber

/**
 * Queries device codec capabilities from Android's [MediaCodecList].
 */
class MediaCodecQuery(
	private val mediaCodecList: MediaCodecList,
	private val softwareCodecsEnabled: Boolean,
) {
	private val MediaCodecInfo.isSoftwareCodec: Boolean
		get() = AndroidVersion.isAtLeastQ && isSoftwareOnly

	private fun decoderInfos(): Sequence<MediaCodecInfo> =
		mediaCodecList.codecInfos.asSequence()
			.filter { !it.isEncoder }
			.filter { softwareCodecsEnabled || !it.isSoftwareCodec }

	fun hasCodecForMime(mime: String): Boolean {
		val info = decoderInfos().firstOrNull { info ->
			info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
		} ?: return false

		Timber.i("found codec %s for mime %s", info.name, mime)
		return true
	}

	fun hasDecoder(mime: String, profile: Int, level: Int): Boolean =
		decoderInfos().any { info -> supportsProfileLevel(info, mime, profile, level) }

	fun getDecoderLevel(mime: String, profile: Int): Int {
		var maxLevel = 0

		for (info in decoderInfos()) {
			val capabilities = getCapabilitiesOrNull(info, mime) ?: continue
			for (profileLevel in capabilities.profileLevels) {
				if (profileLevel.profile == profile) {
					maxLevel = maxOf(maxLevel, profileLevel.level)
				}
			}
		}

		return maxLevel
	}

	/**
	 * The largest frame size a decoder for [mime] accepts. See [selectMaxResolution].
	 */
	fun getMaxResolution(mime: String): Size {
		val candidates = decoderInfos()
			.mapNotNull { info ->
				val vc = getCapabilitiesOrNull(info, mime)?.videoCapabilities ?: return@mapNotNull null
				val width = vc.supportedWidths?.upper ?: return@mapNotNull null
				val height = try {
					vc.getSupportedHeightsFor(width)?.upper
				} catch (_: IllegalArgumentException) {
					vc.supportedHeights?.upper
				} ?: return@mapNotNull null
				DecoderResolution(info.name, info.isSoftwareCodec, width, height)
			}
			.toList()

		val best = selectMaxResolution(candidates)
		if (best == null) {
			Timber.d("No decoder with video capabilities for %s", mime)
			return Size(0, 0)
		}

		Timber.d("Computed max resolution for %s: %dx%d (%s)", mime, best.width, best.height, best.decoder)
		return Size(best.width, best.height)
	}

	fun supportsMultiInstance(mime: String): Boolean =
		decoderInfos().any { info -> hasMultipleInstances(info, mime) }

	private fun supportsProfileLevel(info: MediaCodecInfo, mime: String, profile: Int, level: Int): Boolean {
		val capabilities = getCapabilitiesOrNull(info, mime) ?: return false

		return capabilities.profileLevels.any { profileLevel ->
			profileLevel.profile == profile && meetsLevelRequirement(mime, profileLevel.level, level)
		}
	}

	private fun meetsLevelRequirement(mime: String, decoderLevel: Int, requiredLevel: Int): Boolean {
		// H.263 levels are not completely ordered:
		// Level45 support only implies Level10 support
		if (mime.equals(MediaFormat.MIMETYPE_VIDEO_H263, ignoreCase = true)) {
			if (decoderLevel != requiredLevel &&
				decoderLevel == CodecProfileLevel.H263Level45 &&
				requiredLevel > CodecProfileLevel.H263Level10
			) {
				return false
			}
		}

		return decoderLevel >= requiredLevel
	}

	private fun hasMultipleInstances(info: MediaCodecInfo, mime: String): Boolean {
		if (!info.supportedTypes.contains(mime)) return false
		val capabilities = getCapabilitiesOrNull(info, mime) ?: return false
		return capabilities.maxSupportedInstances > 1
	}

	private fun getCapabilitiesOrNull(
		info: MediaCodecInfo,
		mime: String,
	): MediaCodecInfo.CodecCapabilities? = try {
		info.getCapabilitiesForType(mime)
	} catch (_: IllegalArgumentException) {
		null
	}
}

/**
 * The widest frame a decoder accepts: its largest width, with the largest height it supports at that width.
 */
data class DecoderResolution(
	val decoder: String,
	val isSoftware: Boolean,
	val width: Int,
	val height: Int,
)

/**
 * Picks the resolution to advertise from the decoders of one mime type.
 *
 * Width and height come from the same decoder, and hardware decoders are preferred: playback selects a hardware
 * decoder first, so the larger limits of a software decoder must not be advertised while a hardware decoder
 * exists. Combining the largest width and the largest height of different decoders produced sizes that no
 * decoder supports, e.g. 2048x1152 on a device whose hardware HEVC decoder stops at 1920x1088, so the server
 * transcoded to a size the player then failed to decode.
 */
fun selectMaxResolution(candidates: List<DecoderResolution>): DecoderResolution? =
	candidates.filterNot { it.isSoftware }
		.ifEmpty { candidates }
		.maxByOrNull { it.width.toLong() * it.height }
