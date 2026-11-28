package org.jellyfin.androidtv.ui.itemdetail

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UserItemDataDto
import java.util.UUID

private fun playlistItem(played: Boolean, playbackPositionTicks: Long = 0) = BaseItemDto(
	id = UUID.randomUUID(),
	type = BaseItemKind.EPISODE,
	userData = UserItemDataDto(
		playbackPositionTicks = playbackPositionTicks,
		playCount = if (played) 1 else 0,
		isFavorite = false,
		played = played,
		key = "",
		itemId = UUID.randomUUID(),
	),
)

class PlaylistResumeTest : FunSpec({
	test("firstUnplayedIndex() returns 0 when nothing is played") {
		firstUnplayedIndex(listOf(playlistItem(false), playlistItem(false))) shouldBe 0
	}

	test("firstUnplayedIndex() skips played entries") {
		firstUnplayedIndex(listOf(playlistItem(true), playlistItem(true), playlistItem(false))) shouldBe 2
	}

	test("firstUnplayedIndex() returns a partially played entry") {
		firstUnplayedIndex(listOf(playlistItem(true), playlistItem(false, 1234))) shouldBe 1
	}

	test("firstUnplayedIndex() returns -1 when every entry is played") {
		firstUnplayedIndex(listOf(playlistItem(true), playlistItem(true))) shouldBe -1
	}

	test("firstUnplayedIndex() treats missing user data as unplayed") {
		firstUnplayedIndex(listOf(BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE))) shouldBe 0
	}

	test("firstUnplayedIndex() returns -1 for an empty playlist") {
		firstUnplayedIndex(emptyList()) shouldBe -1
	}
})
