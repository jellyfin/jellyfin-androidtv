package org.jellyfin.androidtv.preference

import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.sdk.api.client.ApiClient
import kotlin.collections.set

/**
 * Repository to access special preference stores.
 */
class PreferencesRepository(
	private val api: ApiClient,
	private val liveTvPreferences: LiveTvPreferences,
	private val userSettingPreferences: UserSettingPreferences,
) {
	private val libraryPreferences = mutableMapOf<String, LibraryPreferences>()

	fun getLibraryPreferences(preferencesId: String): LibraryPreferences {
		val defaultImageType = if (preferencesId == LibraryPreferences.MUSIC_VIDEO_ARTIST_DISPLAY_PREFERENCES_ID ||
			preferencesId == LibraryPreferences.MUSIC_VIDEO_ITEM_DISPLAY_PREFERENCES_ID
		) {
			ImageType.THUMB
		} else {
			ImageType.POSTER
		}
		val store = libraryPreferences[preferencesId] ?: LibraryPreferences(preferencesId, api, defaultImageType)

		libraryPreferences[preferencesId] = store

		// FIXME: Make [getLibraryPreferences] suspended when usages are converted to Kotlin
		if (store.shouldUpdate) runBlocking { store.update() }

		return store
	}

	suspend fun onSessionChanged() {
		// Note: Do not run parallel as the server can't deal with that
		// Relevant server issue: https://github.com/jellyfin/jellyfin/issues/5261
		liveTvPreferences.update()
		userSettingPreferences.update()

		libraryPreferences.clear()
	}
}
