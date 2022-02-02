package org.jellyfin.androidtv.preference

import org.jellyfin.androidtv.constant.GridDirection
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.constant.PosterSize
import org.jellyfin.apiclient.model.entities.SortOrder
import org.jellyfin.sdk.api.client.ApiClient

class LibraryPreferences(
	displayPreferencesId: String,
	api: ApiClient,
) : DisplayPreferencesStore(
	displayPreferencesId = displayPreferencesId,
	api = api,
) {
	companion object {
		val posterSize = Preference.enum("PosterSize", PosterSize.AUTO)
		val imageType = Preference.enum("ImageType", ImageType.DEFAULT)
		val gridDirection = Preference.enum("GridDirection", GridDirection.HORIZONTAL)
		val enableSmartScreen = Preference.boolean("SmartScreen", false)

		// Filters
		val filterFavoritesOnly = Preference.boolean("FilterFavoritesOnly", false)
		val filterUnwatchedOnly = Preference.boolean("FilterUnwatchedOnly", false)

		// Item sorting
		val sortBy = Preference.string("SortBy", "SortName")
		val sortOrder = Preference.enum("SortOrder", SortOrder.Ascending)
	}
}
