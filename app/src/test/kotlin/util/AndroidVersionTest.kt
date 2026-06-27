package org.jellyfin.androidtv.util

import android.os.Build
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject

class AndroidVersionTest : FunSpec({
	beforeEach {
		mockkObject(AndroidVersion)
	}

	afterEach {
		unmockkObject(AndroidVersion)
	}

	test("version checks return true when sdkInt is at the threshold") {
		every { AndroidVersion.sdkInt } returns Build.VERSION_CODES.Q

		AndroidVersion.isAtLeastN shouldBe true
		AndroidVersion.isAtLeastO shouldBe true
		AndroidVersion.isAtLeastP shouldBe true
		AndroidVersion.isAtLeastQ shouldBe true
		AndroidVersion.isAtLeastR shouldBe false
		AndroidVersion.isAtLeastS shouldBe false
		AndroidVersion.isAtLeastT shouldBe false
	}

	test("version checks return false when sdkInt is one below the threshold") {
		every { AndroidVersion.sdkInt } returns Build.VERSION_CODES.R - 1

		AndroidVersion.isAtLeastR shouldBe false
		AndroidVersion.isAtLeastQ shouldBe true
	}

	test("all checks are true on the newest mocked version") {
		every { AndroidVersion.sdkInt } returns Build.VERSION_CODES.BAKLAVA

		AndroidVersion.isAtLeastN shouldBe true
		AndroidVersion.isAtLeastO shouldBe true
		AndroidVersion.isAtLeastP shouldBe true
		AndroidVersion.isAtLeastQ shouldBe true
		AndroidVersion.isAtLeastR shouldBe true
		AndroidVersion.isAtLeastS shouldBe true
		AndroidVersion.isAtLeastT shouldBe true
		AndroidVersion.isAtLeastUpsideDownCake shouldBe true
		AndroidVersion.isAtLeastBaklava shouldBe true
	}

	test("all checks are false on a version below every threshold") {
		every { AndroidVersion.sdkInt } returns Build.VERSION_CODES.M

		AndroidVersion.isAtLeastN shouldBe false
		AndroidVersion.isAtLeastO shouldBe false
		AndroidVersion.isAtLeastP shouldBe false
		AndroidVersion.isAtLeastQ shouldBe false
		AndroidVersion.isAtLeastR shouldBe false
		AndroidVersion.isAtLeastS shouldBe false
		AndroidVersion.isAtLeastT shouldBe false
		AndroidVersion.isAtLeastUpsideDownCake shouldBe false
		AndroidVersion.isAtLeastBaklava shouldBe false
	}
})
