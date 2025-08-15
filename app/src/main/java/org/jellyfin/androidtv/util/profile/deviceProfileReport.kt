package org.jellyfin.androidtv.util.profile

import android.content.Context
import android.media.MediaCodecList
import android.os.Build
import android.util.Range
import android.view.Display
import android.view.Surface
import androidx.core.content.ContextCompat
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.appendCodeBlock
import org.jellyfin.androidtv.util.appendDetails
import org.jellyfin.androidtv.util.appendItem
import org.jellyfin.androidtv.util.appendSection
import org.jellyfin.androidtv.util.appendValue
import org.jellyfin.androidtv.util.buildMarkdown
import org.jellyfin.sdk.api.client.util.ApiSerializer
import kotlin.time.Duration.Companion.nanoseconds

private val prettyPrintJson = Json { prettyPrint = true }
private fun formatJson(json: String) = prettyPrintJson.encodeToString(prettyPrintJson.parseToJsonElement(json))

private fun Range<Int>.prettyFormat() = if (lower == upper) "$lower" else "$lower-$upper"

// Names are copied from MediaCodecInfo.CodecCapabilities.FEATURE_xxx constants as some constants are not available in older API versions
private val featureNames = setOf(
	"adaptive-playback",
	"detached-surface",
	"dynamic-color-aspects",
	"dynamic-timestamp",
	"encoding-statistics",
	"frame-parsing",
	"hdr-editing",
	"hlg-editing",
	"intra-refresh",
	"low-latency",
	"multiple-frames",
	"partial-frame",
	"qp-bounds",
	"region-of-interest",
	"secure-playback",
	"tunneled-playback",
)

// API levels
private val isN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N // API 24
private val isO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O // API 26
private val isQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q // API 29
private val isR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R // API 30
private val isS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S // API 31
private val isUpsideDownCake = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE // API 34
private val isBaklava = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA // API 36

fun createDeviceProfileReport(
	context: Context,
	userPreferences: UserPreferences,
) = buildMarkdown {
	// Header
	appendLine("---")
	appendLine("client: Jellyfin for Android TV")
	appendLine("client_version: ${BuildConfig.VERSION_NAME}")
	appendLine("client_repository: https://github.com/jellyfin/jellyfin-androidtv")
	appendLine("type: media_capabilities_report")
	appendLine("format: markdown")
	appendLine("---")
	appendLine()

	// Device profile send to server
	appendDetails("Generated device profile") {
		appendCodeBlock(
			language = "json",
			code = createDeviceProfile(userPreferences, disableDirectPlay = false)
				.let(ApiSerializer::encodeRequestBody)
				?.let(::formatJson)
		)
	}

	// Device capabilities used to generate profile
	val codecs = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
		.filter { !it.isEncoder }
		.sortedBy { if (isQ) it.canonicalName else it.name }

	appendDetails("Device codec decoders") {
		for (codec in codecs) {
			if (isQ) appendLine("- **${codec.canonicalName} (${codec.name})**")
			else appendLine("- **${codec.name}**")

			if (isQ) appendLine("  - isVendor: ${codec.isVendor}")
			if (isQ) appendLine("  - isHardwareAccelerated: ${codec.isHardwareAccelerated}")
			if (isQ) appendLine("  - isSoftwareOnly: ${codec.isSoftwareOnly}")
			if (isQ) appendLine("  - isAlias: ${codec.isAlias}")

			for (type in codec.supportedTypes) {
				val capabilities = codec.getCapabilitiesForType(type)

				appendLine("  - **$type**")

				capabilities.audioCapabilities?.let { audio ->
					if (isS) appendLine("    - minInputChannelCount: ${audio.minInputChannelCount}")
					appendLine("    - maxInputChannelCount: ${audio.maxInputChannelCount}")
					if (isS) audio.inputChannelCountRanges.takeIf { it.isNotEmpty() }?.let {
						appendLine("    - inputChannelCountRanges: ${it.joinToString(", ") { it.prettyFormat() }}")
					}
					audio.bitrateRange?.let { appendLine("    - bitrateRange: ${it.prettyFormat()}") }
					audio.supportedSampleRates?.takeIf { it.isNotEmpty() }?.let {
						appendLine("    - supportedSampleRates: ${it.joinToString(", ")}")
					}
					audio.supportedSampleRateRanges?.takeIf { it.isNotEmpty() }?.let {
						appendLine("    - supportedSampleRateRanges: ${it.joinToString(", ") { it.prettyFormat() }}")
					}
				}

				capabilities.videoCapabilities?.let { video ->
					video.bitrateRange?.let { appendLine("    - bitrateRange: ${it.prettyFormat()}") }
					video.supportedFrameRates?.let { appendLine("    - supportedFrameRates: ${it.prettyFormat()}") }
					appendLine("    - widthAlignment: ${video.widthAlignment}")
					appendLine("    - heightAlignment: ${video.heightAlignment}")
					video.supportedWidths?.let {
						appendLine("    - supportedWidths: ${it.prettyFormat()}")
					}
					video.supportedHeights?.let {
						appendLine("    - supportedHeights: ${it.prettyFormat()}")
					}
					if (isQ) video.supportedPerformancePoints?.takeIf { it.isNotEmpty() }?.let {
						appendLine("    - supportedPerformancePoints: ${it.joinToString(", ")}")
					}
				}

				capabilities.colorFormats?.takeIf { it.isNotEmpty() }?.let { colorFormats ->
					appendLine("    - colorFormats: ${colorFormats.joinToString(", ")}")
				}

				capabilities.profileLevels?.takeIf { it.isNotEmpty() }?.let { profileLevels ->
					appendLine("    - profileLevels")
					for (profileLevel in profileLevels) {
						appendLine("      - ${profileLevel.profile}: ${profileLevel.level}")
					}
				}

				// Only show features section if there is at least 1
				featureNames.mapNotNull { name ->
					when {
						capabilities.isFeatureRequired(name) -> "$name (required)"
						capabilities.isFeatureSupported(name) -> name
						else -> null
					}
				}.takeIf { it.isNotEmpty() }?.let { features ->
					appendLine("    - features")
					for (feature in features) {
						appendLine("      - $feature")
					}
				}
			}

			appendLine()
		}
	}

	appendDetails("Known media types") {
		codecs
			.flatMap { codec -> codec.supportedTypes.asIterable() }
			.distinct()
			.sorted()
			.forEach { type -> appendLine("- $type") }
	}

	appendDetails("Display information") {
		val display = ContextCompat.getDisplayOrDefault(context)

		// Basic information
		appendItem("Id") { appendValue(display.displayId.toString()) }
		appendItem("Name") { appendValue(display.name) }
		if (isS && display.deviceProductInfo != null) {
			val productInfo = requireNotNull(display.deviceProductInfo)
			appendItem("Display product id") { appendValue(productInfo.productId) }
			appendItem("Display bame") { appendValue(productInfo.name) }
			appendItem("Display manufacture year") { appendValue(productInfo.manufactureYear.toString()) }
			appendItem("Display manufacture week") { appendValue(productInfo.manufactureWeek.toString()) }
			appendItem("Display manufacturer pnp id") { appendValue(productInfo.manufacturerPnpId) }
			appendItem("Display model year") { appendValue(productInfo.modelYear.toString()) }
		}
		appendItem("Rotation") {
			when (display.rotation) {
				Surface.ROTATION_0 -> append("0°")
				Surface.ROTATION_90 -> append("90°")
				Surface.ROTATION_180 -> append("180°")
				Surface.ROTATION_270 -> append("270°")
				else -> appendValue("Unknown (${display.rotation}")
			}
		}

		// Refresh rate and timing
		appendItem("Refresh rate") { appendValue(display.refreshRate.toString()) }
		if (isBaklava) appendItem("Adaptive refresh rate") { appendValue(display.hasArrSupport().toString()) }
		appendItem("VSYNC offset") { appendValue(display.appVsyncOffsetNanos.nanoseconds.toString()) }
		appendItem("Presentation deadline") { appendValue(display.presentationDeadlineNanos.nanoseconds.toString()) }
		if (isR) appendItem("Minimal post processing") { appendValue(display.isMinimalPostProcessingSupported.toString()) }

		// HDR
		if (isO) appendItem("Any HDR") { appendValue(display.isHdr.toString()) }
		if (isO) appendItem("Wide color gamut") { appendValue(display.isWideColorGamut.toString()) }
		if (isQ) appendItem("Preferred wide color space") { appendValue(display.preferredWideGamutColorSpace.toString()) }
		if (isN) {
			val supportedHdrTypes = if (isUpsideDownCake) display.mode.supportedHdrTypes.toList()
			else display.hdrCapabilities.supportedHdrTypes.toList()

			appendItem("HDR capabilities") {
				appendLine()
				appendLine("- Dolby Vision: ${supportedHdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION)}")
				appendLine("- HDR10: ${supportedHdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_HDR10)}")
				if (isQ) appendLine("- HDR10+: ${supportedHdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS)}")
				appendLine("- HLG: ${supportedHdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_HLG)}")
			}
		}

		if (isUpsideDownCake) appendItem("HDR/SDR ratio") {
			appendLine()
			appendItem("Available") { appendValue(display.isHdrSdrRatioAvailable.toString()) }
			appendItem("Ratio") { appendValue(display.hdrSdrRatio.toString()) }
			if (isBaklava) {
				appendItem("Highest ratio") { appendValue(display.highestHdrSdrRatio.toString()) }
			}
		}
	}

	appendSection("App information") {
		appendItem("App version") {
			appendValue(BuildConfig.VERSION_NAME)
			append(" (")
			appendValue(BuildConfig.VERSION_CODE.toString())
			append(")")
		}
		appendItem("Package name") { appendValue(context.packageName) }
	}

	appendSection("Device information") {
		appendItem("Android version") { appendValue(Build.VERSION.RELEASE) }
		appendItem("Device brand") { appendValue(Build.BRAND) }
		appendItem("Device product") { appendValue(Build.PRODUCT) }
		appendItem("Device model") { appendValue(Build.MODEL) }
	}
}
