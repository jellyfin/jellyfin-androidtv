package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

class LetterboxSubtitlePlacementTests : FunSpec({
	fun wide(captionHeight: Int = 90, margin: Int = 8, inset: Int = 0) =
		LetterboxSubtitlePlacement.calculate(1920, 1080, 0, 140, 1920, 940, captionHeight, margin, inset)

	test("two-line caption fits inside the lower bar without changing font size") {
		val placement = wide().shouldNotBeNull()
		placement.top shouldBe 940
		placement.height shouldBe 140
		placement.inset shouldBe 8
	}

	test("existing offset is clamped to the available bar space") {
		wide(inset = 86).shouldNotBeNull().inset shouldBe 42
	}

	test("an exact fit keeps a margin on both sides") {
		wide(captionHeight = 124, inset = 86).shouldNotBeNull().inset shouldBe 8
	}

	test("a caption taller than the bar keeps its bottom lines in the bar") {
		val placement = wide(captionHeight = 200, inset = 86).shouldNotBeNull()
		placement.top shouldBe 864
		placement.height shouldBe 216
		placement.inset shouldBe 8
	}

	test("one pixel too tall for the bar anchors at the lowest safe position") {
		val placement = wide(captionHeight = 125).shouldNotBeNull()
		placement.top shouldBe 1080 - 141
		placement.inset shouldBe 8
	}

	test("captions taller than half the screen fall back to the video renderer") {
		wide(captionHeight = 524).shouldNotBeNull()
		wide(captionHeight = 525).shouldBeNull()
	}

	test("full-frame video and encoded bars cannot be inferred from geometry") {
		LetterboxSubtitlePlacement.calculate(1920, 1080, 0, 0, 1920, 1080, 50, 8, 0).shouldBeNull()
	}

	test("pillarboxed and contracted video windows do not qualify") {
		LetterboxSubtitlePlacement.calculate(1920, 1080, 240, 0, 1680, 1080, 50, 8, 0).shouldBeNull()
		LetterboxSubtitlePlacement.calculate(1920, 1080, 320, 140, 1600, 940, 50, 8, 0).shouldBeNull()
	}

	test("an invalid or zero-size video surface falls back") {
		LetterboxSubtitlePlacement.calculate(1920, 1080, 0, 540, 1920, 540, 50, 8, 0).shouldBeNull()
		LetterboxSubtitlePlacement.calculate(1920, 1080, -1, 140, 1920, 940, 50, 8, 0).shouldBeNull()
		LetterboxSubtitlePlacement.calculate(1920, 1080, 0, 140, 1920, 1081, 50, 8, 0).shouldBeNull()
		LetterboxSubtitlePlacement.calculate(0, 1080, 0, 140, 1920, 940, 50, 8, 0).shouldBeNull()
	}

	test("viewport rounding permits one or two pixels, but not an unrelated bottom gap") {
		LetterboxSubtitlePlacement.calculate(1920, 1080, 0, 141, 1920, 940, 50, 8, 0).shouldNotBeNull()
		LetterboxSubtitlePlacement.calculate(1920, 1080, 0, 142, 1920, 940, 50, 8, 0).shouldNotBeNull()
		LetterboxSubtitlePlacement.calculate(1920, 1080, 0, 143, 1920, 940, 50, 8, 0).shouldBeNull()
	}

	test("negative values and excessive margins cannot yield a clipped caption") {
		wide(captionHeight = -1).shouldBeNull()
		wide(margin = -1).shouldBeNull()
		wide(margin = Int.MAX_VALUE).shouldBeNull()
		wide(inset = -1).shouldBeNull()
		wide(inset = Int.MAX_VALUE).shouldNotBeNull().inset shouldBe 42
	}

	test("encoded bars use the measured picture rows and a wider tolerance") {
		val placement = LetterboxSubtitlePlacement.bottom(1080, 138, 940, 21, 90, 8, 0).shouldNotBeNull()
		placement.top shouldBe 940
		placement.inset shouldBe 8
		LetterboxSubtitlePlacement.bottom(1080, 100, 940, 21, 90, 8, 0).shouldBeNull()
	}

	test("top captions sit below the top edge and extend downward when tall") {
		val fits = LetterboxSubtitlePlacement.top(1080, 140, 940, 2, 60, 8).shouldNotBeNull()
		fits.top shouldBe 0
		fits.height shouldBe 140
		fits.inset shouldBe 8
		LetterboxSubtitlePlacement.top(1080, 140, 940, 2, 200, 8).shouldNotBeNull().height shouldBe 216
		LetterboxSubtitlePlacement.top(1080, 140, 940, 2, 525, 8).shouldBeNull()
		LetterboxSubtitlePlacement.top(1080, 0, 1080, 2, 60, 8).shouldBeNull()
	}
})
