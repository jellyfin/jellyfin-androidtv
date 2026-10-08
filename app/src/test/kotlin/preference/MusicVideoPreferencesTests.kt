package org.jellyfin.androidtv.preference

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.constant.PosterSize

class MusicVideoPreferencesTests : FunSpec({
	test("landscape default preserves explicit image type and size selections") {
		val preferences = LibraryPreferences(LibraryPreferences.MUSIC_VIDEO_ITEM_DISPLAY_PREFERENCES_ID, mockk(), ImageType.THUMB)

		preferences[preferences.imageType] shouldBe ImageType.THUMB
		preferences[preferences.imageType] = ImageType.POSTER
		preferences[LibraryPreferences.posterSize] = PosterSize.LARGE

		preferences[preferences.imageType] shouldBe ImageType.POSTER
		preferences[LibraryPreferences.posterSize] shouldBe PosterSize.LARGE
	}

	test("artist and item preferences remain independent") {
		val artists = LibraryPreferences(LibraryPreferences.MUSIC_VIDEO_ARTIST_DISPLAY_PREFERENCES_ID, mockk(), ImageType.THUMB)
		val items = LibraryPreferences(LibraryPreferences.MUSIC_VIDEO_ITEM_DISPLAY_PREFERENCES_ID, mockk(), ImageType.THUMB)

		artists[artists.imageType] = ImageType.BANNER
		artists[LibraryPreferences.posterSize] = PosterSize.SMALL

		items[items.imageType] shouldBe ImageType.THUMB
		items[LibraryPreferences.posterSize] shouldBe PosterSize.MED
	}

	test("ordinary library preferences retain the poster default") {
		val preferences = LibraryPreferences("ordinary-library", mockk())
		preferences[preferences.imageType] shouldBe ImageType.POSTER
	}
})
