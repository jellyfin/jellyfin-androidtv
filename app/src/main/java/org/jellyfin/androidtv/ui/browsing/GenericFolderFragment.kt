package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.querying.GetSpecialsRequest
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest

class GenericFolderFragment : EnhancedBrowseFragment() {
	companion object {
		private val showSpecialViewTypes = setOf(
			BaseItemKind.COLLECTION_FOLDER,
			BaseItemKind.FOLDER,
			BaseItemKind.USER_VIEW,
			BaseItemKind.CHANNEL_FOLDER_ITEM,
		)
	}

	override fun setupQueries(rowLoader: RowLoader) {
		if ((mFolder.childCount == null || mFolder.childCount == 0) && !setOf(
				BaseItemKind.CHANNEL,
				BaseItemKind.CHANNEL_FOLDER_ITEM,
				BaseItemKind.USER_VIEW,
				BaseItemKind.COLLECTION_FOLDER,
			).contains(mFolder.type)
		) return

		if (showSpecialViewTypes.contains(mFolder.type)) {
			if (mFolder.type != BaseItemKind.CHANNEL_FOLDER_ITEM) {
				val resume = GetItemsRequest(
					fields = ItemRepository.itemFields,
					parentId = mFolder.id,
					limit = 50,
					filters = setOf(ItemFilter.IS_RESUMABLE),
					sortBy = setOf(ItemSortBy.DATE_PLAYED),
					sortOrder = setOf(SortOrder.DESCENDING),
				)
				mRows.add(BrowseRowDef(getString(R.string.lbl_continue_watching), resume, 0))
			}

			val latest = GetItemsRequest(
				fields = ItemRepository.itemFields,
				parentId = mFolder.id,
				limit = 50,
				filters = setOf(ItemFilter.IS_UNPLAYED),
				sortBy = setOf(ItemSortBy.DATE_CREATED),
				sortOrder = setOf(SortOrder.DESCENDING),
			)
			mRows.add(BrowseRowDef(getString(R.string.lbl_latest), latest, 0))
		}

		val byName = GetItemsRequest(
			fields = ItemRepository.itemFields,
			parentId = mFolder.id,
		)
		val header = when (mFolder.type) {
			BaseItemKind.SEASON -> mFolder.name
			else -> getString(R.string.lbl_by_name)
		}

		mRows.add(BrowseRowDef(header, byName, 100))

		if (mFolder.type == BaseItemKind.SEASON) {
			val specials = GetSpecialsRequest(mFolder.id)
			mRows.add(BrowseRowDef(getString(R.string.lbl_specials), specials))
		}

		rowLoader.loadRows(mRows)
	}
}
