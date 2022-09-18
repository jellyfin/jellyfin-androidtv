package org.jellyfin.androidtv.ui.browsing

interface RowLoader {
	fun loadRows(rows: MutableList<BrowseRowDef>)
}
