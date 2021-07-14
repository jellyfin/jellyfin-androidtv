package org.jellyfin.androidtv.preference

import org.jellyfin.sdk.api.client.KtorClient
import org.jellyfin.sdk.api.operations.DisplayPreferencesApi
import kotlin.collections.set

/**
 * Repository to access special preference stores.
 */
class PreferencesRepository(
	private val apiClient: KtorClient,
	private val liveTvPreferences: LiveTvPreferences,
) {
	private val displayPreferencesApi = DisplayPreferencesApi(apiClient)
	private val libraryPreferences = mutableMapOf<String, LibraryPreferences>()

	public fun getLibraryPreferences(preferencesId: String): LibraryPreferences {
		val store = libraryPreferences[preferencesId]
			?: LibraryPreferences(preferencesId, displayPreferencesApi)

		libraryPreferences[preferencesId] = store

		// FIXME: Make [getLibraryPreferences] suspended when usages are converted to Kotlin
		if (store.shouldUpdate) store.updateBlocking()

		return store
	}

	public fun onSessionChanged() {
		liveTvPreferences.clearCache()
		libraryPreferences.clear()
	}
}
