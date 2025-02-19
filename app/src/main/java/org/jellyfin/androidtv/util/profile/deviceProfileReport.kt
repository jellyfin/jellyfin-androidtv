package org.jellyfin.androidtv.util.profile

import android.content.Context
import android.media.MediaCodecList
import android.os.Build
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.appendCodeBlock
import org.jellyfin.androidtv.util.appendItem
import org.jellyfin.androidtv.util.appendSection
import org.jellyfin.androidtv.util.appendValue
import org.jellyfin.androidtv.util.buildMarkdown
import org.jellyfin.sdk.api.client.util.ApiSerializer

private val prettyPrintJson = Json { prettyPrint = true }
private fun formatJson(json: String) = prettyPrintJson.encodeToString(prettyPrintJson.parseToJsonElement(json))

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
	appendSection("Generated device profile") {
		appendCodeBlock(
			language = "json",
			code = createDeviceProfile(userPreferences, disableDirectPlay = false)
				.let(ApiSerializer::encodeRequestBody)
				?.let(::formatJson)
		)
	}

	// Device capabilities used to generate profile
	appendSection("Device codec decoders") {
		val isQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
		val isS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

		val codecs = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
			.filter { !it.isEncoder }
			.sortedBy { if (isQ) it.canonicalName else it.name }

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
					appendLine("    - bitrateRange: ${audio.bitrateRange}")
					if (isS) appendLine("    - inputChannelCountRanges: ${audio.inputChannelCountRanges.joinToString(", ")}")
					appendLine("    - maxInputChannelCount: ${audio.maxInputChannelCount}")
					if (isS) appendLine("    - minInputChannelCount: ${audio.minInputChannelCount}")
					appendLine("    - supportedSampleRateRanges: ${audio.supportedSampleRateRanges?.joinToString(", ")}")
					appendLine("    - supportedSampleRates: ${audio.supportedSampleRates?.joinToString(", ")}")
				}

				capabilities.videoCapabilities?.let { video ->
					appendLine("    - bitrateRange: ${video.bitrateRange}")
					appendLine("    - supportedFrameRates: ${video.supportedFrameRates}")
					appendLine("    - widthAlignment: ${video.widthAlignment}")
					appendLine("    - heightAlignment: ${video.heightAlignment}")
					appendLine("    - supportedWidths: ${video.supportedWidths}")
					appendLine("    - supportedHeights: ${video.supportedHeights}")
					if (isQ) appendLine("    - supportedPerformancePoints: ${video.supportedPerformancePoints?.joinToString(", ")}")
				}
			}

			appendLine()
		}

		appendSection("Known media types", depth = 4) {
			codecs
				.flatMap { codec -> codec.supportedTypes.asIterable() }
				.distinct()
				.sorted()
				.forEach { type -> appendLine("- $type") }
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
