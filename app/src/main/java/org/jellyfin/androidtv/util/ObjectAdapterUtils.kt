package org.jellyfin.androidtv.util

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ListRow

/**
 * Add a list row to the adapter only if it contains at least one item
 */
fun ArrayObjectAdapter.addIfNotEmpty(row: ListRow) {
	if (row.adapter.size() > 0) add(row)
}
