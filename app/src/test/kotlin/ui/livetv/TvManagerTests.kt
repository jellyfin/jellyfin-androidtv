package org.jellyfin.androidtv.ui.livetv

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class TvManagerTests : FunSpec({
	val channelsField = TvManager::class.java.getDeclaredField("allChannels").apply { isAccessible = true }
	var previousChannels: Any? = null

	beforeTest {
		previousChannels = channelsField.get(null)
		channelsField.set(null, null)
	}

	afterTest {
		channelsField.set(null, previousChannels)
	}

	test("missing channel cache returns an empty list and no channel") {
		TvManager.getAllChannels() shouldBe emptyList()
		TvManager.getChannel(0) shouldBe null
		TvManager.getChannel(-1) shouldBe null
	}

	test("empty channel cache returns no channel") {
		channelsField.set(null, emptyList<BaseItemDto>())
		TvManager.getChannel(0) shouldBe null
	}

	test("cached channel lookup preserves valid channels and rejects invalid indices") {
		val channel = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.TV_CHANNEL)
		channelsField.set(null, listOf(channel))
		TvManager.getChannel(0) shouldBe channel
		TvManager.getChannel(-1) shouldBe null
		TvManager.getChannel(1) shouldBe null
		TvManager.getChannel(Int.MAX_VALUE) shouldBe null
	}
})
