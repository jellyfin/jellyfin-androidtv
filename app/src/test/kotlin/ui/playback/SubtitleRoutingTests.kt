package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod

class SubtitleRoutingTests : FunSpec({
	test("ASS and SSA direct subtitle tracks use libass routing") {
		usesLibassSubtitleRendering("ass", SubtitleDeliveryMethod.EMBED) shouldBe true
		usesLibassSubtitleRendering("ssa", SubtitleDeliveryMethod.EXTERNAL) shouldBe true
		usesLibassSubtitleRendering("ASS", SubtitleDeliveryMethod.EXTERNAL) shouldBe true
	}

	test("non-ASS or baked subtitle tracks keep existing routing") {
		usesLibassSubtitleRendering("srt", SubtitleDeliveryMethod.EXTERNAL) shouldBe false
		usesLibassSubtitleRendering("vtt", SubtitleDeliveryMethod.HLS) shouldBe false
		usesLibassSubtitleRendering("pgs", SubtitleDeliveryMethod.EMBED) shouldBe false
		usesLibassSubtitleRendering("ass", SubtitleDeliveryMethod.ENCODE) shouldBe false
		usesLibassSubtitleRendering(null, SubtitleDeliveryMethod.EXTERNAL) shouldBe false
	}
})
