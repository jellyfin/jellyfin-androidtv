package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.request.GetItemsRequest

class CollectionFragment : EnhancedBrowseFragment() {
	companion object {
		private val itemFields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.OVERVIEW,
			ItemFields.ITEM_COUNTS,
			ItemFields.DISPLAY_PREFERENCES_ID,
			ItemFields.CHILD_COUNT,
			ItemFields.MEDIA_STREAMS,
			ItemFields.MEDIA_SOURCES,
		)
	}

	override fun setupQueries(rowLoader: RowLoader) {
		val movies = GetItemsRequest(
			fields = itemFields,
			parentId = mFolder.id,
			includeItemTypes = setOf(BaseItemKind.MOVIE),
		)
		mRows.add(BrowseRowDef(getString(R.string.lbl_movies), movies, 100))

		val series = GetItemsRequest(
			fields = itemFields,
			parentId = mFolder.id,
			includeItemTypes = setOf(BaseItemKind.SERIES),
		)
		mRows.add(BrowseRowDef(getString(R.string.lbl_tv_series), series, 100))

		val others = GetItemsRequest(
			fields = itemFields,
			parentId = mFolder.id,
			excludeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
		)
		mRows.add(BrowseRowDef(getString(R.string.lbl_other), others, 100))

		rowLoader.loadRows(mRows)
	}
}
