package org.jellyfin.playback.media3.exoplayer.subtitle

import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.SubtitleDecoder
import androidx.media3.extractor.text.SubtitleDecoderException
import androidx.media3.extractor.text.SubtitleInputBuffer
import androidx.media3.extractor.text.SubtitleOutputBuffer

@UnstableApi
internal class OffsetSubtitleDecoder(
	private val delegate: SubtitleDecoder,
	private val offsetState: SubtitleTimingOffsetState,
) : SubtitleDecoder {
	override fun getName(): String = delegate.name

	override fun setOutputStartTimeUs(outputStartTimeUs: Long) {
		delegate.setOutputStartTimeUs(outputStartTimeUs)
	}

	override fun dequeueInputBuffer(): SubtitleInputBuffer? = delegate.dequeueInputBuffer()

	override fun queueInputBuffer(inputBuffer: SubtitleInputBuffer) {
		delegate.queueInputBuffer(inputBuffer)
	}

	override fun dequeueOutputBuffer(): SubtitleOutputBuffer? = delegate.dequeueOutputBuffer()?.let { buffer ->
		OffsetSubtitleOutputBuffer(buffer, offsetState)
	}

	override fun flush() {
		delegate.flush()
	}

	override fun release() {
		delegate.release()
	}

	override fun setPositionUs(positionUs: Long) {
		delegate.setPositionUs(positionUs)
	}
}
