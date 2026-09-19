package org.jellyfin.androidtv.ui.playback

import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.video.MediaCodecVideoDecoderException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private fun codecInfo(name: String) =
	MediaCodecInfo.newInstance(name, "video/hevc", "video/hevc", null, true, false, true, false, false)

class FailedVideoDecoderTrackerTests : FunSpec({
	test("filter returns the list unchanged when nothing is excluded") {
		val tracker = FailedVideoDecoderTracker()
		val decoders = listOf(codecInfo("OMX.MTK.VIDEO.DECODER.HEVC"), codecInfo("c2.android.hevc.decoder"))

		tracker.filter(decoders) shouldContainExactly decoders
	}

	test("tryExcludeFailedDecoder ignores errors unrelated to a decoder") {
		val tracker = FailedVideoDecoderTracker()

		tracker.tryExcludeFailedDecoder(RuntimeException("network error")) shouldBe false
	}

	test("tryExcludeFailedDecoder ignores a decoder exception with no codec info") {
		val tracker = FailedVideoDecoderTracker()
		val error = MediaCodecVideoDecoderException(RuntimeException(), null, null)

		tracker.tryExcludeFailedDecoder(error) shouldBe false
	}

	test("tryExcludeFailedDecoder finds the exception anywhere in the cause chain") {
		val tracker = FailedVideoDecoderTracker()
		val failedDecoder = codecInfo("OMX.MTK.VIDEO.DECODER.HEVC")
		val decoderException = MediaCodecVideoDecoderException(IllegalStateException(), failedDecoder, null)
		val playbackException = RuntimeException("ExoPlaybackException", decoderException)

		tracker.tryExcludeFailedDecoder(playbackException) shouldBe true
		tracker.filter(listOf(failedDecoder)) shouldContainExactly emptyList()
	}

	test("tryExcludeFailedDecoder returns false when the same decoder fails again") {
		val tracker = FailedVideoDecoderTracker()
		val failedDecoder = codecInfo("OMX.MTK.VIDEO.DECODER.HEVC")
		val error = MediaCodecVideoDecoderException(RuntimeException(), failedDecoder, null)

		tracker.tryExcludeFailedDecoder(error) shouldBe true
		tracker.tryExcludeFailedDecoder(error) shouldBe false
	}

	test("only the failed decoder is excluded, others remain candidates") {
		val tracker = FailedVideoDecoderTracker()
		val failedDecoder = codecInfo("OMX.MTK.VIDEO.DECODER.HEVC")
		val otherDecoder = codecInfo("c2.android.hevc.decoder")
		val error = MediaCodecVideoDecoderException(RuntimeException(), failedDecoder, null)

		tracker.tryExcludeFailedDecoder(error)

		tracker.filter(listOf(failedDecoder, otherDecoder)) shouldContainExactly listOf(otherDecoder)
	}

	test("clear allows a previously failed decoder to be selected again") {
		val tracker = FailedVideoDecoderTracker()
		val failedDecoder = codecInfo("OMX.MTK.VIDEO.DECODER.HEVC")
		val error = MediaCodecVideoDecoderException(RuntimeException(), failedDecoder, null)

		tracker.tryExcludeFailedDecoder(error)
		tracker.clear()

		tracker.filter(listOf(failedDecoder)) shouldContainExactly listOf(failedDecoder)
	}
})
