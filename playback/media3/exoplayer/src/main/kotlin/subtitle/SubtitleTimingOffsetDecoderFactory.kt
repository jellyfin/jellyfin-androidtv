package org.jellyfin.playback.media3.exoplayer.subtitle

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.text.SubtitleDecoderFactory
import androidx.media3.extractor.text.SubtitleDecoder
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.cea.Cea608Decoder
import androidx.media3.extractor.text.cea.Cea708Decoder

@UnstableApi
class SubtitleTimingOffsetDecoderFactory(
	private val subtitleParserFactory: SubtitleParser.Factory,
	private val offsetState: SubtitleTimingOffsetState,
) : SubtitleDecoderFactory {
	private val defaultDecoderFactory = SubtitleDecoderFactory.DEFAULT
	override fun supportsFormat(format: Format): Boolean =
		defaultDecoderFactory.supportsFormat(format) || when (format.sampleMimeType) {
			MimeTypes.APPLICATION_CEA608,
			MimeTypes.APPLICATION_MP4CEA608,
			MimeTypes.APPLICATION_CEA708 -> true
			else -> subtitleParserFactory.supportsFormat(format)
		}

	override fun createDecoder(format: Format): SubtitleDecoder {
		val mimeType = format.sampleMimeType
		val delegate = when {
			defaultDecoderFactory.supportsFormat(format) -> defaultDecoderFactory.createDecoder(format)

			mimeType == MimeTypes.APPLICATION_CEA608 || mimeType == MimeTypes.APPLICATION_MP4CEA608 -> Cea608Decoder(
				mimeType,
				format.accessibilityChannel,
				Cea608Decoder.MIN_DATA_CHANNEL_TIMEOUT_MS,
			)

			mimeType == MimeTypes.APPLICATION_CEA708 -> Cea708Decoder(format.accessibilityChannel, format.initializationData)

			subtitleParserFactory.supportsFormat(format) -> {
				val parser = subtitleParserFactory.create(format)
				ParserBackedSubtitleDecoder("${parser.javaClass.simpleName}Decoder", parser)
			}

			else -> throw IllegalArgumentException("Attempted to create decoder for unsupported MIME type: $mimeType")
		}

		return when {
			isSubtitleTimingOffsetSupported(format) -> OffsetSubtitleDecoder(delegate, offsetState)
			else -> delegate
		}
	}
}
