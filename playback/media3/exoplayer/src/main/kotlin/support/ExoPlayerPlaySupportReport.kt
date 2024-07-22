package org.jellyfin.playback.media3.exoplayer.support

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.BaseRenderer
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RendererCapabilities
import org.jellyfin.playback.core.support.PlaySupportReport

data class ExoPlayerPlaySupportReport(
	val format: FormatSupport?,
	val adaptive: AdaptiveSupport?,
	val tunneling: Boolean?,
	val hardwareAcceleration: Boolean?,
	val decoder: DecoderSupport?,
) : PlaySupportReport {
	override val canPlay = format == FormatSupport.HANDLED || tunneling == true || hardwareAcceleration == true

	companion object {
		@OptIn(UnstableApi::class)
		fun fromFlags(flags: Int): ExoPlayerPlaySupportReport = ExoPlayerPlaySupportReport(
			format = FormatSupport.fromFlags(RendererCapabilities.getFormatSupport(flags)),
			adaptive = AdaptiveSupport.fromFlags(RendererCapabilities.getAdaptiveSupport(flags)),
			tunneling = tunnelingFromFlags(RendererCapabilities.getTunnelingSupport(flags)),
			hardwareAcceleration = hardwareAccelerationFromFlags(RendererCapabilities.getHardwareAccelerationSupport(flags)),
			decoder = DecoderSupport.fromFlags(RendererCapabilities.getDecoderSupport(flags)),
		)

		@OptIn(UnstableApi::class)
		private fun tunnelingFromFlags(flags: Int) = when (RendererCapabilities.getTunnelingSupport(flags)) {
			RendererCapabilities.TUNNELING_SUPPORTED -> true
			RendererCapabilities.TUNNELING_NOT_SUPPORTED -> false
			else -> null
		}

		@OptIn(UnstableApi::class)
		private fun hardwareAccelerationFromFlags(flags: Int) = when (RendererCapabilities.getHardwareAccelerationSupport(flags)) {
			RendererCapabilities.HARDWARE_ACCELERATION_SUPPORTED -> true
			RendererCapabilities.HARDWARE_ACCELERATION_NOT_SUPPORTED -> false
			else -> null
		}
	}
}

fun ExoPlayer.getPlaySupportReport(format: Format): ExoPlayerPlaySupportReport =
	ExoPlayerPlaySupportReport.fromFlags(supportsFormat(format))

fun ExoPlayer.getPlaySupportReport(formats: Collection<Format>): ExoPlayerPlaySupportReport = formats
	.map { format -> supportsFormat(format) }
	.reduce { acc, i -> acc and i }
	.let { flags -> ExoPlayerPlaySupportReport.fromFlags(flags) }


@OptIn(UnstableApi::class)
fun ExoPlayer.supportsFormat(format: Format): Int {
	var capabilities = 0

	repeat(rendererCount) { rendererIndex ->
		val renderer = getRenderer(rendererIndex)

		val rendererCapabilities = when (renderer) {
			is BaseRenderer -> renderer.supportsFormat(format)
			else -> renderer.capabilities.supportsFormat(format)
		}

		capabilities = capabilities or rendererCapabilities
	}

	return capabilities
}
