package org.jellyfin.androidtv.preference

import android.content.SharedPreferences
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.preference.constant.HDRSupport
import org.jellyfin.preference.store.SharedPreferenceStore

class HDRSupportPreferenceTests : FunSpec({
	test("HDR support defaults to automatic when no value is stored") {
		val store = testPreferenceStore()

		store[UserPreferences.hdrSupport] shouldBe HDRSupport.AUTOMATIC
		UserPreferences.hdrSupport.key shouldBe "hdr_support"
	}

	test("HDR support values round-trip through the preference store") {
		val store = testPreferenceStore()

		for (mode in HDRSupport.entries) {
			store[UserPreferences.hdrSupport] = mode
			store[UserPreferences.hdrSupport] shouldBe mode
		}
	}
})

private fun testPreferenceStore(): SharedPreferenceStore {
	var storedValue: String? = null
	val editor = mockk<SharedPreferences.Editor>(relaxed = true)
	val sharedPreferences = mockk<SharedPreferences> {
		every { getString("hdr_support", "") } answers { storedValue.orEmpty() }
		every { edit() } returns editor
	}
	every { editor.putString("hdr_support", any()) } answers {
		storedValue = secondArg()
		editor
	}

	return object : SharedPreferenceStore(sharedPreferences) {}
}
