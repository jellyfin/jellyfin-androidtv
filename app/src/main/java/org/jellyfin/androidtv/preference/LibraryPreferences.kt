package org.jellyfin.androidtv.preference

import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.constant.GridDirection
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.constant.PosterSize
import org.jellyfin.apiclient.model.entities.SortOrder
import org.jellyfin.sdk.api.operations.DisplayPreferencesApi
import java.util.*

class LibraryPreferences(
	displayPreferencesId: String,
	userId: UUID,
	displayPreferencesApi: DisplayPreferencesApi,
) : DisplayPreferencesStore(
	displayPreferencesId = displayPreferencesId,
	userId = userId,
	displayPreferencesApi = displayPreferencesApi
) {
	/**
	 * Compatability with old Java classes.
	 */
	fun updateBlocking() = runBlocking { update() }

	/**
	 * Compatability with old Java classes.
	 */
	fun commitBlocking() = runBlocking { commit() }

	companion object {
		val posterSize = Preference.enum("PosterSize", PosterSize.AUTO)
		val imageType = Preference.enum("ImageType", ImageType.DEFAULT)
		val gridDirection = Preference.enum("GridDirection", GridDirection.HORIZONTAL)
		val enableSmartScreen = Preference.boolean("SmartScreen", false)

		// Filters
		val filterFavoritesOnly = Preference.boolean("FilterFavoritesOnly", false)
		val filterUnwatchedOnly = Preference.boolean("FilterUnwatchedOnly", false)

		// Item sorting
		val sortBy = Preference.string("SortBy", "DateCreated,SortName")
		val sortOrder = Preference.enum("SortOrder", SortOrder.Descending)
	}
}
