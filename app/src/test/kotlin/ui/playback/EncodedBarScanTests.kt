package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class EncodedBarScanTests : FunSpec({
	val width = 96
	val height = 270

	// A frame with picture rows [top, bottom) at the given brightness and black elsewhere.
	fun frame(top: Int, bottom: Int, luma: Int = 120) = IntArray(width * height) { i ->
		if (i / width in top until bottom) luma else 0
	}

	fun EncodedBarScan.feed(frame: IntArray, times: Int) = (1..times).map { update(frame, width, height) }

	test("picture rows are found between encoded bars") {
		EncodedBarScan.pictureRows(frame(35, 235), width, height)!!.toList() shouldBe listOf(35, 235)
	}

	test("a dark frame cannot be measured") {
		EncodedBarScan.pictureRows(frame(35, 235, luma = 20), width, height).shouldBeNull()
		EncodedBarScan.pictureRows(frame(120, 150), width, height).shouldBeNull()
	}

	test("a crop is accepted after three matching frames") {
		val scan = EncodedBarScan()
		scan.feed(frame(35, 235), 3) shouldBe listOf(false, false, true)
		scan.topFraction() shouldBe 35f / height
		scan.bottomFraction() shouldBe 235f / height
	}

	test("dark and one-sided frames keep the known crop") {
		val scan = EncodedBarScan()
		scan.feed(frame(35, 235), 3)
		scan.feed(frame(35, 235, luma = 10), 5).any { it } shouldBe false
		// A dark lower part of the scene looks like a taller bottom bar.
		scan.feed(frame(35, 180), 5).any { it } shouldBe false
		scan.topFraction() shouldBe 35f / height
	}

	test("a full-frame picture replaces the crop") {
		val scan = EncodedBarScan()
		scan.feed(frame(35, 235), 3)
		scan.feed(frame(0, height), 3).last() shouldBe true
		scan.topFraction() shouldBe 0f
		scan.bottomFraction() shouldBe 1f
	}

	test("thin symmetric dark edges count as a full-frame picture, not bars") {
		val scan = EncodedBarScan()
		scan.feed(frame(5, height - 5), 5)
		scan.topFraction() shouldBe 0f
		scan.bottomFraction() shouldBe 1f
	}
})
