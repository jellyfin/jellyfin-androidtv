package org.jellyfin.playback.exoplayer.support

import com.google.android.exoplayer2.BaseRenderer
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.RendererCapabilities
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
		fun fromFlags(flags: Int): ExoPlayerPlaySupportReport = ExoPlayerPlaySupportReport(
			format = FormatSupport.fromFlags(RendererCapabilities.getFormatSupport(flags)),
			adaptive = AdaptiveSupport.fromFlags(RendererCapabilities.getAdaptiveSupport(flags)),
			tunneling = tunnelingFromFlags(RendererCapabilities.getTunnelingSupport(flags)),
			hardwareAcceleration = hardwareAccelerationFromFlags(RendererCapabilities.getHardwareAccelerationSupport(flags)),
			decoder = DecoderSupport.fromFlags(RendererCapabilities.getDecoderSupport(flags)),
		)

		private fun tunnelingFromFlags(flags: Int) = when (RendererCapabilities.getTunnelingSupport(flags)) {
			RendererCapabilities.TUNNELING_SUPPORTED -> true
			RendererCapabilities.TUNNELING_NOT_SUPPORTED -> false
			else -> null
		}

		private fun hardwareAccelerationFromFlags(flags: Int) = when (RendererCapabilities.getHardwareAccelerationSupport(flags)) {
			RendererCapabilities.HARDWARE_ACCELERATION_SUPPORTED -> true
			RendererCapabilities.HARDWARE_ACCELERATION_NOT_SUPPORTED -> false
			else -> null
		}
	}
}

fun ExoPlayer.getPlaySupportReport(format: Format): ExoPlayerPlaySupportReport =
	ExoPlayerPlaySupportReport.fromFlags(supportsFormat(format))

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
