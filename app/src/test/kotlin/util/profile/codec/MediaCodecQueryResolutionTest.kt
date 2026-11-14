package org.jellyfin.androidtv.util.profile.codec

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MediaCodecQueryResolutionTest : FunSpec({
	test("width and height come from the same decoder") {
		val candidates = listOf(
			DecoderResolution("hw.wide", isSoftware = false, width = 4096, height = 1080),
			DecoderResolution("hw.tall", isSoftware = false, width = 1920, height = 2160),
		)
		// Not 4096x2160, which neither decoder supports
		selectMaxResolution(candidates) shouldBe candidates[0]
	}

	test("a hardware decoder wins over a larger software decoder") {
		// A projector: hardware HEVC up to 1920x1088, the platform software decoder up to 2048x2048
		val hardware = DecoderResolution("OMX.MS.HEVC.Decoder", isSoftware = false, width = 1920, height = 1088)
		val software = DecoderResolution("c2.android.hevc.decoder", isSoftware = true, width = 2048, height = 2048)
		selectMaxResolution(listOf(software, hardware)) shouldBe hardware
	}

	test("software decoders are used when there is no hardware decoder") {
		val software = DecoderResolution("c2.android.av1.decoder", isSoftware = true, width = 2048, height = 2048)
		selectMaxResolution(listOf(software)) shouldBe software
	}

	test("the largest hardware decoder is chosen") {
		val small = DecoderResolution("hw.small", isSoftware = false, width = 1920, height = 1088)
		val large = DecoderResolution("hw.large", isSoftware = false, width = 3840, height = 2160)
		selectMaxResolution(listOf(small, large)) shouldBe large
	}

	test("no decoders gives no resolution") {
		selectMaxResolution(emptyList()) shouldBe null
	}
})
