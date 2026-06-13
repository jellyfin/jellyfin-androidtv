package org.jellyfin.androidtv.util.profile.codec

import android.media.MediaCodecInfo.CodecProfileLevel
import android.os.Build
import androidx.media3.common.MimeTypes

class Av1CodecCapabilities(
	private val query: MediaCodecQuery,
) {
	companion object {
		private const val MIME_AV1 = MimeTypes.VIDEO_AV1
		private const val MIME_DOLBY_VISION = MimeTypes.VIDEO_DOLBY_VISION

		// Fallback values from AOSP MediaCodecInfo.java for pre-Q/R devices
		// Some devices (e.g., Fire OS) support AV1 below the official API level
		internal const val AV1_PROFILE_MAIN10 = 0x2
		internal const val AV1_PROFILE_MAIN10_HDR10 = 0x1000
		internal const val AV1_PROFILE_MAIN10_HDR10_PLUS = 0x2000
		internal const val AV1_LEVEL5 = 0x1000
		internal const val DV_PROFILE_DVAV1_10 = 0x400
	}

	private val profileMain10: Int
		get() = if (DeviceSdk.sdkInt >= Build.VERSION_CODES.Q) CodecProfileLevel.AV1ProfileMain10 else AV1_PROFILE_MAIN10

	private val profileMain10HDR10: Int
		get() = if (DeviceSdk.sdkInt >= Build.VERSION_CODES.Q) CodecProfileLevel.AV1ProfileMain10HDR10 else AV1_PROFILE_MAIN10_HDR10

	private val profileMain10HDR10Plus: Int
		get() = if (DeviceSdk.sdkInt >= Build.VERSION_CODES.Q) CodecProfileLevel.AV1ProfileMain10HDR10Plus else AV1_PROFILE_MAIN10_HDR10_PLUS

	private val dolbyVisionProfile10: Int
		get() = if (DeviceSdk.sdkInt >= Build.VERSION_CODES.R) CodecProfileLevel.DolbyVisionProfileDvav110 else DV_PROFILE_DVAV1_10

	private val level5: Int
		get() = if (DeviceSdk.sdkInt >= Build.VERSION_CODES.Q) CodecProfileLevel.AV1Level5 else AV1_LEVEL5

	fun supportsAv1(): Boolean = query.hasCodecForMime(MIME_AV1)

	fun supportsAv1Main10(): Boolean = query.hasDecoder(
		MIME_AV1,
		profileMain10,
		level5,
	)

	fun supportsAv1DolbyVision(): Boolean =
		DeviceSdk.sdkInt >= Build.VERSION_CODES.N &&
			query.hasDecoder(
				MIME_DOLBY_VISION,
				dolbyVisionProfile10,
				CodecProfileLevel.DolbyVisionLevelHd24,
			)

	fun supportsAv1HDR10(): Boolean = query.hasDecoder(
		MIME_AV1,
		profileMain10HDR10,
		level5,
	)

	fun supportsAv1HDR10Plus(): Boolean = query.hasDecoder(
		MIME_AV1,
		profileMain10HDR10Plus,
		level5,
	)
}
