package org.jellyfin.androidtv.data.model

import org.jellyfin.apiclient.model.querying.ItemFilter

class FilterOptions {
	var isFavoriteOnly = false
	var isUnwatchedOnly = false

	val filters: Array<ItemFilter>
		get() {
			if (!isUnwatchedOnly && !isFavoriteOnly) return emptyArray()

			return buildList {
				if (isFavoriteOnly) add(ItemFilter.IsFavorite)
				if (isUnwatchedOnly) add(ItemFilter.IsUnplayed)
			}.toTypedArray()
		}
}
