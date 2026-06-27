package org.jellyfin.playback.media3.exoplayer.subtitle

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.SubtitleOutputBuffer

@UnstableApi
internal class OffsetSubtitleOutputBuffer(
	private val delegate: SubtitleOutputBuffer,
	private val offsetState: SubtitleTimingOffsetState,
) : SubtitleOutputBuffer(), SubtitleTimingOffsetState.Listener {
	init {
		skippedOutputBufferCount = delegate.skippedOutputBufferCount
		shouldBeSkipped = delegate.shouldBeSkipped
		if (delegate.isEndOfStream) addFlag(C.BUFFER_FLAG_END_OF_STREAM)
		if (delegate.isFirstSample) addFlag(C.BUFFER_FLAG_FIRST_SAMPLE)
		if (delegate.isKeyFrame) addFlag(C.BUFFER_FLAG_KEY_FRAME)
		if (delegate.isLastSample) addFlag(C.BUFFER_FLAG_LAST_SAMPLE)
		if (delegate.hasSupplementalData()) addFlag(C.BUFFER_FLAG_HAS_SUPPLEMENTAL_DATA)
		if (delegate.notDependedOn()) addFlag(C.BUFFER_FLAG_NOT_DEPENDED_ON)
		offsetState.addListener(this)
	}

	override fun onSubtitleTimingOffsetChanged(offsetUs: Long) {
		if (!delegate.isEndOfStream) {
			updateContent(offsetUs)
		}
	}

	private fun updateContent(offsetUs: Long) {
		setContent(delegate.timeUs + offsetUs, delegate, offsetUs)
	}

	override fun release() {
		offsetState.removeListener(this)
		delegate.release()
	}
}
