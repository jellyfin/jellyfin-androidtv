package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.request.GetItemsRequest

class ByLetterFragment : BrowseFolderFragment() {
	override suspend fun setupQueries(rowLoader: RowLoader) {
		val childCount = folder?.childCount ?: 0
		if (childCount <= 0) return

		val letters = getString(R.string.byletter_letters)

		// Add a '#' item
		val numbersItemsRequest = GetItemsRequest(
			parentId = folder?.id,
			sortBy = setOf(ItemSortBy.SORT_NAME),
			includeItemTypes = includeType?.let(BaseItemKind::fromNameOrNull)?.let(::setOf),
			nameLessThan = letters.substring(0, 1),
			recursive = true,
			fields = ItemRepository.itemFields,
		)

		rows.add(BrowseRowDef("#", numbersItemsRequest, 40))

		// Add all the defined letters
		for (letter in letters.toCharArray()) {
			val letterItemsRequest = GetItemsRequest(
				parentId = folder?.id,
				sortBy = setOf(ItemSortBy.SORT_NAME),
				includeItemTypes = includeType?.let(BaseItemKind::fromNameOrNull)?.let(::setOf),
				nameStartsWith = letter.toString(),
				recursive = true,
				fields = ItemRepository.itemFields,
			)

			rows.add(BrowseRowDef(letter.toString(), letterItemsRequest, 40))
		}

		rowLoader.loadRows(rows)
	}
}
