package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TimeUtilsTests : FunSpec({
	test("TimeUtils.secondsToMillis() works correctly") {
		TimeUtils.secondsToMillis(0.0) shouldBe 0
		TimeUtils.secondsToMillis(1.0) shouldBe 1000
		TimeUtils.secondsToMillis(1.25) shouldBe 1250
	}

	test("TimeUtils.formatMillis() works correctly") {
		TimeUtils.formatMillis(0) shouldBe "0:00"
		TimeUtils.formatMillis(13000) shouldBe "0:13"
		TimeUtils.formatMillis(300000) shouldBe "5:00"
		TimeUtils.formatMillis(541000) shouldBe "9:01"
		TimeUtils.formatMillis(599000) shouldBe "9:59"
		TimeUtils.formatMillis(1560000) shouldBe "26:00"
		TimeUtils.formatMillis(1561000) shouldBe "26:01"
		TimeUtils.formatMillis(1603000) shouldBe "26:43"
		TimeUtils.formatMillis(3600000) shouldBe "1:00:00"
		TimeUtils.formatMillis(3661000) shouldBe "1:01:01"
		TimeUtils.formatMillis(4155000) shouldBe "1:09:15"
		TimeUtils.formatMillis(4563489) shouldBe "1:16:03"
		TimeUtils.formatMillis(43200000) shouldBe "12:00:00"
	}
})
