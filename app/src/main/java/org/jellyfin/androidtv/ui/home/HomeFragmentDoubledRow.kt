package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.ChangeTriggerType
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest

class HomeFragmentDoubledRow(
	private val userRepository: UserRepository,
	private val userViews: Collection<BaseItemDto>,
	private val releasedFirst: Boolean,
) : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		// Get configuration (to find excluded items)
		val configuration = userRepository.currentUser.value?.configuration

		// Create a list of views to include
		val latestItemsExcludes = configuration?.latestItemsExcludes.orEmpty()
		userViews
			.filterNot { item -> item.collectionType in EXCLUDED_COLLECTION_TYPES || item.id in latestItemsExcludes }
			.forEach { item ->
				// Create rows based on the order specified
				if (releasedFirst) {
					// Recently Released first, then Recently Added
					addRecentlyReleasedRow(context, cardPresenter, rowsAdapter, item)
					addRecentlyAddedRow(context, cardPresenter, rowsAdapter, item)
				} else {
					// Recently Added first, then Recently Released
					addRecentlyAddedRow(context, cardPresenter, rowsAdapter, item)
					addRecentlyReleasedRow(context, cardPresenter, rowsAdapter, item)
				}
			}
	}

	private fun addRecentlyReleasedRow(
		context: Context,
		cardPresenter: CardPresenter,
		rowsAdapter: MutableObjectAdapter<Row>,
		item: BaseItemDto
	) {
		val request = GetItemsRequest(
			fields = ItemRepository.itemFields,
			imageTypeLimit = 1,
			parentId = item.id,
			recursive = true,
			limit = ITEM_LIMIT,
			sortBy = setOf(ItemSortBy.PREMIERE_DATE),
			sortOrder = setOf(SortOrder.DESCENDING),
		)

		val title = context.getString(R.string.lbl_recently_released_in, item.name)
		val row = HomeFragmentBrowseRowDefRow(BrowseRowDef(title, request, 0, false, true, arrayOf(ChangeTriggerType.LibraryUpdated)))
		row.addToRowsAdapter(context, cardPresenter, rowsAdapter)
	}

	private fun addRecentlyAddedRow(
		context: Context,
		cardPresenter: CardPresenter,
		rowsAdapter: MutableObjectAdapter<Row>,
		item: BaseItemDto
	) {
		val request = GetLatestMediaRequest(
			fields = ItemRepository.itemFields,
			imageTypeLimit = 1,
			parentId = item.id,
			groupItems = true,
			limit = ITEM_LIMIT,
		)

		val title = context.getString(R.string.lbl_latest_in, item.name)
		val row = HomeFragmentBrowseRowDefRow(BrowseRowDef(title, request, arrayOf(ChangeTriggerType.LibraryUpdated)))
		row.addToRowsAdapter(context, cardPresenter, rowsAdapter)
	}

	companion object {
		// Collections excluded from doubled rows
		private val EXCLUDED_COLLECTION_TYPES = arrayOf(
			CollectionType.PLAYLISTS,
			CollectionType.LIVETV,
			CollectionType.BOXSETS,
			CollectionType.BOOKS,
		)

		// Maximum amount of items loaded for a row
		private const val ITEM_LIMIT = 50
	}
}
