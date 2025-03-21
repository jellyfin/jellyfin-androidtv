package org.jellyfin.androidtv.util.profile

import android.content.Context
import android.media.MediaCodecList
import android.os.Build
import android.util.Range
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
	val isQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
	val isS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

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
