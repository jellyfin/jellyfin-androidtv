package org.jellyfin.androidtv.preference

import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.sdk.api.client.KtorClient
import org.jellyfin.sdk.api.operations.DisplayPreferencesApi
import kotlin.collections.set

/**
 * Repository to access special preference stores.
 */
class PreferencesRepository(
	private val sessionRepository: SessionRepository,
	private val apiClient: KtorClient,
) {
	private val libraryPreferences = mutableMapOf<String, LibraryPreferences>()

	public fun getLibraryPreferences(preferencesId: String): LibraryPreferences {
		val store = libraryPreferences[preferencesId] ?: LibraryPreferences(
			preferencesId,
			sessionRepository.currentSession.value!!.userId,
			DisplayPreferencesApi(apiClient)
		)

		libraryPreferences[preferencesId] = store

		// FIXME: Make [getLibraryPreferences] suspended when usages are converted to Kotlin
		if (store.shouldUpdate) store.updateBlocking()

		return store
	}
}
