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

	fun getMaxResolution(mime: String): Size {
		val resolutions = decoderInfos()
			.mapNotNull { info ->
				val vc = getCapabilitiesOrNull(info, mime)?.videoCapabilities ?: return@mapNotNull null
				val width = vc.supportedWidths?.upper ?: return@mapNotNull null
				val height = vc.supportedHeights?.upper ?: return@mapNotNull null
				if (width <= 0 || height <= 0) return@mapNotNull null

				DecoderResolution(width, height, info.isSoftwareCodec)
			}.toList()

		val (maxWidth, maxHeight) = selectMaxResolution(resolutions)

		Timber.d("Computed max resolution for %s: %dx%d", mime, maxWidth, maxHeight)
		return Size(maxWidth, maxHeight)
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

internal data class DecoderResolution(
	val width: Int,
	val height: Int,
	val isSoftware: Boolean,
)

/**
 * Prefer hardware decoder limits for the device profile. Software decoder limits
 * are only used when no hardware decoder reported a usable resolution.
 */
internal fun selectMaxResolution(resolutions: List<DecoderResolution>): Pair<Int, Int> {
	val hardware = resolutions.filterNot { it.isSoftware }
	val candidates = hardware.ifEmpty { resolutions }
	return (candidates.maxOfOrNull { it.width } ?: 0) to
		(candidates.maxOfOrNull { it.height } ?: 0)
}
