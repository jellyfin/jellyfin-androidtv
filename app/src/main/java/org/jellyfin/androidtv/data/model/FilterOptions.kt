package org.jellyfin.androidtv.data.model

import org.jellyfin.sdk.model.api.ItemFilter


class FilterOptions {
	var isFavoriteOnly = false
	var isUnwatchedOnly = false
	var selectedGenre: String? = null

	val filters: Set<ItemFilter>
		get() = buildSet {
			if (isFavoriteOnly) add(ItemFilter.IS_FAVORITE)
			if (isUnwatchedOnly) add(ItemFilter.IS_UNPLAYED)
		}

	val genres: Set<String>?
		get() = selectedGenre
			?.takeIf { it.isNotBlank() }
			?.let(::setOf)

	fun hasGenre() = !selectedGenre.isNullOrBlank()

	fun hasFilters() = isFavoriteOnly || isUnwatchedOnly || hasGenre()
}
