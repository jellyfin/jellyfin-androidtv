package org.jellyfin.androidtv.preference

import kotlinx.coroutines.runBlocking
import org.jellyfin.sdk.api.client.ApiClient

/**
 * Repository to access special preference stores.
 */
class PreferencesRepository(
	private val api: ApiClient,
	private val liveTvPreferences: LiveTvPreferences,
	private val userSettingPreferences: UserSettingPreferences,
) {
	private val libraryPreferences = mutableMapOf<String, LibraryPreferences>()

	// FIXME: Remove the blocking API when the remaining callers support suspension.
	fun getLibraryPreferences(preferencesId: String): LibraryPreferences = runBlocking {
		loadLibraryPreferences(preferencesId)
	}

	suspend fun loadLibraryPreferences(preferencesId: String): LibraryPreferences {
		val store = libraryPreferences[preferencesId] ?: LibraryPreferences(preferencesId, api)

		libraryPreferences[preferencesId] = store

		if (store.shouldUpdate) store.update()

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
