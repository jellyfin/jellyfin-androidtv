package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class BaseItemDtoBaseRowItemTests : FunSpec({
	val context = mockk<Context>()

	test("season card shows series title and season subtitle when requested") {
		val season = BaseItemDto(
			id = UUID.randomUUID(),
			type = BaseItemKind.SEASON,
			name = "Season 1",
			seriesName = "Example Series",
		)

		val rowItem = BaseItemDtoBaseRowItem(
			item = season,
			showParentTitle = true,
		)

		rowItem.getCardName(context) shouldBe "Example Series"
		rowItem.getSubText(context) shouldBe "Season 1"
	}

	test("season card does not duplicate season name when series title is missing") {
		val season = BaseItemDto(
			id = UUID.randomUUID(),
			type = BaseItemKind.SEASON,
			name = "Season 1",
		)

		val rowItem = BaseItemDtoBaseRowItem(
			item = season,
			showParentTitle = true,
		)

		rowItem.getCardName(context) shouldBe "Season 1"
		rowItem.getSubText(context) shouldBe ""
	}
})
