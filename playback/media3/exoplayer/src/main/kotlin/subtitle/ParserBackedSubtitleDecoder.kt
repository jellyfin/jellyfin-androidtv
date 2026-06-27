package org.jellyfin.playback.media3.exoplayer.subtitle

import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.SimpleSubtitleDecoder
import androidx.media3.extractor.text.Subtitle
import androidx.media3.extractor.text.SubtitleParser

@UnstableApi
internal class ParserBackedSubtitleDecoder(
	name: String,
	private val subtitleParser: SubtitleParser,
) : SimpleSubtitleDecoder(name) {
	override fun decode(data: ByteArray, length: Int, reset: Boolean): Subtitle {
		if (reset) subtitleParser.reset()
		return subtitleParser.parseToLegacySubtitle(data, 0, length)
	}
}
