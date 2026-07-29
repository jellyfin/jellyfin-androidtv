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
			.mapNotNull { info -> getCapabilitiesOrNull(info, mime)?.videoCapabilities }
			.mapNotNull { vc ->
				val w = vc.supportedWidths?.upper ?: return@mapNotNull null
				val h = vc.supportedHeights?.upper ?: return@mapNotNull null
				w to h
			}

		val maxWidth = resolutions.maxOfOrNull { it.first } ?: 0
		val maxHeight = resolutions.maxOfOrNull { it.second } ?: 0

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
