package org.jellyfin.androidtv.data.compat

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class VideoOptionsTests : FunSpec({
	test("audio stream index is preserved when restarting the same item") {
		val itemId = UUID.randomUUID()
		val options = VideoOptions().apply {
			this.itemId = itemId
			audioStreamIndex = 2
		}

		options.getAudioStreamIndex(itemId) shouldBe 2
	}

	test("audio stream index is not carried over to the next item") {
		val options = VideoOptions().apply {
			itemId = UUID.randomUUID()
			audioStreamIndex = 1
		}

		options.getAudioStreamIndex(UUID.randomUUID()) shouldBe null
	}
})
