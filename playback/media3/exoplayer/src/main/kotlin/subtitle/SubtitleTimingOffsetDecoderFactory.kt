package org.jellyfin.playback.media3.exoplayer.subtitle

import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.text.SubtitleDecoderFactory
import androidx.media3.extractor.text.SubtitleDecoder
import androidx.media3.extractor.text.SubtitleParser

@UnstableApi
class SubtitleTimingOffsetDecoderFactory(
	private val subtitleParserFactory: SubtitleParser.Factory,
	private val offsetState: SubtitleTimingOffsetState,
) : SubtitleDecoderFactory {
	private val defaultDecoderFactory = SubtitleDecoderFactory.DEFAULT
	override fun supportsFormat(format: Format): Boolean =
		defaultDecoderFactory.supportsFormat(format) || subtitleParserFactory.supportsFormat(format)

	override fun createDecoder(format: Format): SubtitleDecoder {
		val delegate = when {
			defaultDecoderFactory.supportsFormat(format) -> defaultDecoderFactory.createDecoder(format)

			subtitleParserFactory.supportsFormat(format) -> {
				val parser = subtitleParserFactory.create(format)
				ParserBackedSubtitleDecoder("${parser.javaClass.simpleName}Decoder", parser)
			}

			else -> throw IllegalArgumentException(
				"Attempted to create decoder for unsupported MIME type: ${format.sampleMimeType}"
			)
		}

		return when {
			isSubtitleTimingOffsetSupported(format) -> OffsetSubtitleDecoder(delegate, offsetState)
			else -> delegate
		}
	}
}
