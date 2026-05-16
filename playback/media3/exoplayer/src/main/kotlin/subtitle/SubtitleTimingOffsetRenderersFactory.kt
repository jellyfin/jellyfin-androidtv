package org.jellyfin.playback.media3.exoplayer.subtitle

import android.content.Context
import android.os.Looper
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.extractor.text.SubtitleParser

@UnstableApi
class SubtitleTimingOffsetRenderersFactory(
	context: Context,
	private val offsetState: SubtitleTimingOffsetState,
	private val subtitleParserFactory: SubtitleParser.Factory,
) : DefaultRenderersFactory(context) {
	override fun buildTextRenderers(
		context: Context,
		output: TextOutput,
		outputLooper: Looper,
		extensionRendererMode: Int,
		out: ArrayList<Renderer>,
	) {
		out += TextRenderer(
			output,
			outputLooper,
			SubtitleTimingOffsetDecoderFactory(subtitleParserFactory, offsetState)
		).apply {
			@Suppress("DEPRECATION")
			experimentalSetLegacyDecodingEnabled(true)
		}
	}
}
