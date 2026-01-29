package org.jellyfin.androidtv.preference

import org.jellyfin.androidtv.constant.GridDirection
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.constant.PosterSize
import org.jellyfin.androidtv.constant.ViewMode
import org.jellyfin.androidtv.preference.store.DisplayPreferencesStore
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.enumPreference
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder

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

		// View mode preference - GRID or LIST
		val viewMode = enumPreference("ViewMode", ViewMode.GRID)

		// List view specific settings
		val listItemHeight = enumPreference("ListItemHeight", ListItemHeight.MEDIUM)

		// Filters
		val filterFavoritesOnly = booleanPreference("FilterFavoritesOnly", false)
		val filterUnwatchedOnly = booleanPreference("FilterUnwatchedOnly", false)

		// Item sorting
		val sortBy = enumPreference("SortBy", ItemSortBy.SORT_NAME)
		val sortOrder = enumPreference("SortOrder", SortOrder.ASCENDING)
	}
}

/**
 * Height options for list view items
 */
enum class ListItemHeight(val heightDp: Int, val nameRes: Int) {
	SMALL(80, org.jellyfin.androidtv.R.string.list_item_height_small),
	MEDIUM(120, org.jellyfin.androidtv.R.string.list_item_height_medium),
	LARGE(160, org.jellyfin.androidtv.R.string.list_item_height_large),
}
