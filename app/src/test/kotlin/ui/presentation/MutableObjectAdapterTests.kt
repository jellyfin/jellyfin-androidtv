package org.jellyfin.androidtv.ui.presentation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class MutableObjectAdapterTests : FunSpec({
	test("hideAt and showAt preserve original order when shown in reverse") {
		val adapter = MutableObjectAdapter<String>()
		listOf("a", "b", "c", "d", "e").forEach(adapter::add)

		adapter.hideAt(1) shouldBe true // hides "b"
		adapter.hideAt(3) shouldBe true // hides "e"

		adapter.showAt(4) shouldBe true // show "e" first
		adapter.showAt(1) shouldBe true // show "b" second

		adapter.toList().shouldContainExactly("a", "b", "c", "d", "e")
		adapter.getHiddenItemsCount() shouldBe 0
	}

	test("show by element restores hidden items without reordering") {
		val adapter = MutableObjectAdapter<String>()
		listOf("a", "b", "c", "d", "e").forEach(adapter::add)

		adapter.hide("b") shouldBe true
		adapter.hide("e") shouldBe true

		adapter.show("e") shouldBe true
		adapter.show("b") shouldBe true

		adapter.toList().shouldContainExactly("a", "b", "c", "d", "e")
		adapter.getHiddenItemsCount() shouldBe 0
	}

	test("showAt returns false for non-hidden index") {
		val adapter = MutableObjectAdapter<String>()
		listOf("a", "b", "c").forEach(adapter::add)

		adapter.hideAt(1) shouldBe true
		adapter.showAt(0) shouldBe false
		adapter.showAt(1) shouldBe true
		adapter.showAt(1) shouldBe false
	}
})

