package org.jellyfin.androidtv.preference

import org.jellyfin.androidtv.constant.GridDirection
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.constant.PosterSize
import org.jellyfin.androidtv.preference.store.DisplayPreferencesStore
import org.jellyfin.apiclient.model.entities.SortOrder
import org.jellyfin.apiclient.model.querying.ItemSortBy
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.enumPreference
import org.jellyfin.preference.stringPreference
import org.jellyfin.sdk.api.client.ApiClient

class LibraryPreferences(
	displayPreferencesId: String,
	api: ApiClient,
) : DisplayPreferencesStore(
	displayPreferencesId = displayPreferencesId,
	api = api,
) {
	companion object {
		val posterSize = enumPreference("PosterSize", PosterSize.MED)
		val imageType = enumPreference("ImageType", ImageType.POSTER)
		val gridDirection = enumPreference("GridDirection", GridDirection.HORIZONTAL)
		val enableSmartScreen = booleanPreference("SmartScreen", false)

		// Filters
		val filterFavoritesOnly = booleanPreference("FilterFavoritesOnly", false)
		val filterUnwatchedOnly = booleanPreference("FilterUnwatchedOnly", false)

		// Item sorting
		val sortBy = stringPreference("SortBy", ItemSortBy.SortName)
		val sortOrder = enumPreference("SortOrder", SortOrder.Ascending)
	}
}
