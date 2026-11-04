package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

class SubtitleViewSafetyTests : FunSpec({
	test("zero offset leaves an eight dp safety margin") {
		SubtitleViewSafety.bottomPaddingFraction(800, 2f, 0f) shouldBe (0.02f plusOrMinus 0.00001f)
	}

	test("existing eight percent offset is preserved") {
		SubtitleViewSafety.bottomPaddingFraction(800, 2f, 0.08f) shouldBe (0.08f plusOrMinus 0.00001f)
	}

	test("aspect ratio changes keep the minimum margin constant in pixels") {
		val full = SubtitleViewSafety.bottomPaddingFraction(1080, 2f, 0f)
		val wide = SubtitleViewSafety.bottomPaddingFraction(800, 2f, 0f)
		full * 1080 shouldBe (16f plusOrMinus 0.00001f)
		wide * 800 shouldBe (16f plusOrMinus 0.00001f)
	}

	test("negative offsets use the safety floor") {
		SubtitleViewSafety.bottomPaddingFraction(800, 2f, -0.1f) shouldBe (0.02f plusOrMinus 0.00001f)
	}

	test("unset or invalid geometry does not produce invalid padding") {
		SubtitleViewSafety.bottomPaddingFraction(0, 2f, 0.08f) shouldBe (0.08f plusOrMinus 0.00001f)
		SubtitleViewSafety.bottomPaddingFraction(800, Float.NaN, 0.08f) shouldBe (0.08f plusOrMinus 0.00001f)
		SubtitleViewSafety.bottomPaddingFraction(800, -1f, 0.08f) shouldBe (0.08f plusOrMinus 0.00001f)
		SubtitleViewSafety.bottomPaddingFraction(800, 2f, Float.NaN) shouldBe (0.08f plusOrMinus 0.00001f)
	}

	test("oversized offsets and tiny viewports remain bounded") {
		SubtitleViewSafety.bottomPaddingFraction(800, 2f, 2f) shouldBe (0.8f plusOrMinus 0.00001f)
		SubtitleViewSafety.bottomPaddingFraction(1, 2f, 0f) shouldBe (0.8f plusOrMinus 0.00001f)
	}
})
