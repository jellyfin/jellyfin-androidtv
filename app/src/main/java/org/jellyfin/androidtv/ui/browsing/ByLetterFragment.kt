package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.querying.StdItemQuery
import org.jellyfin.sdk.model.api.ItemSortBy

class ByLetterFragment : BrowseFolderFragment() {
	override suspend fun setupQueries(rowLoader: RowLoader) {
		val childCount = folder?.childCount ?: 0
		if (childCount <= 0) return

		val letters = getString(R.string.byletter_letters)

		// Add a '#' item
		val numbersQuery = StdItemQuery().apply {
			parentId = folder?.id?.toString()
			sortBy = arrayOf(ItemSortBy.SORT_NAME.serialName)
			includeType?.let { includeItemTypes = arrayOf(it) }
			nameLessThan = letters.substring(0, 1)
			recursive = true
		}

		rows.add(BrowseRowDef("#", numbersQuery, 40))

		// Add all the defined letters
		for (letter in letters.toCharArray()) {
			val letterQuery = StdItemQuery().apply {
				parentId = folder?.id?.toString()
				sortBy = arrayOf(ItemSortBy.SORT_NAME.serialName)
				includeType?.let { includeItemTypes = arrayOf(it) }
				nameStartsWith = letter.toString()
				recursive = true
			}

			rows.add(BrowseRowDef(letter.toString(), letterQuery, 40))
		}

		rowLoader.loadRows(rows)
	}
}
