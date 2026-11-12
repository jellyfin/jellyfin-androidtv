package org.jellyfin.androidtv.util.profile.codec

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MediaCodecQueryTest : FunSpec({
	test("hardware decoder resolution is preferred over a larger software decoder resolution") {
		selectMaxResolution(
			listOf(
				DecoderResolution(1920, 1088, isSoftware = false),
				DecoderResolution(2048, 2048, isSoftware = true),
			),
		) shouldBe (1920 to 1088)
	}

	test("maximum width and height are selected from hardware decoders only") {
		selectMaxResolution(
			listOf(
				DecoderResolution(1920, 1080, isSoftware = false),
				DecoderResolution(1280, 1088, isSoftware = false),
				DecoderResolution(4096, 4096, isSoftware = true),
			),
		) shouldBe (1920 to 1088)
	}

	test("software resolution is used when no hardware decoder is available") {
		selectMaxResolution(
			listOf(DecoderResolution(2048, 2048, isSoftware = true)),
		) shouldBe (2048 to 2048)
	}

	test("maximum resolution is selected among software-only decoders") {
		selectMaxResolution(
			listOf(
				DecoderResolution(1920, 1080, isSoftware = true),
				DecoderResolution(2048, 1536, isSoftware = true),
			),
		) shouldBe (2048 to 1536)
	}

	test("empty resolution list returns zero dimensions") {
		selectMaxResolution(emptyList()) shouldBe (0 to 0)
	}
})
