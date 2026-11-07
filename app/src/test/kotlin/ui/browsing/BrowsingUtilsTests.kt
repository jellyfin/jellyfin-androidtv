package org.jellyfin.androidtv.ui.browsing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class BrowsingUtilsTests : FunSpec({
	test("next episodes are anchored to the current item within its series and season") {
		val seriesId = UUID.randomUUID()
		val seasonId = UUID.randomUUID()
		val itemId = UUID.randomUUID()

		val request = BrowsingUtils.createNextEpisodesRequest(seriesId, seasonId, itemId)

		request.seriesId shouldBe seriesId
		request.seasonId shouldBe seasonId
		request.startItemId shouldBe itemId
	}

	test("next episodes skip only the anchored item and retain the twenty episode limit") {
		val request = BrowsingUtils.createNextEpisodesRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())

		request.startIndex shouldBe 1
		request.limit shouldBe 20
	}
})
