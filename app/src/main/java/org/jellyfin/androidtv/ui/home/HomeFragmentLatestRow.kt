package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.Row
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.ChangeTriggerType
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest

class HomeFragmentLatestRow(
	private val userRepository: UserRepository,
	private val userViewsRepository: UserViewsRepository,
) : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		// Get configuration (to find excluded items)
		val configuration = userRepository.currentUser.value?.configuration

		// Create a list of views to include
		val latestItemsExcludes = configuration?.latestItemsExcludes.orEmpty()
		val views = runBlocking { userViewsRepository.views.first() }
		views
			.filterNot { item -> item.collectionType in EXCLUDED_COLLECTION_TYPES || item.id in latestItemsExcludes }
			.map { item ->
				// Create query and add it to a new row
				val request = GetLatestMediaRequest(
					fields = setOf(
						ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
						ItemFields.OVERVIEW,
						ItemFields.CHILD_COUNT,
						ItemFields.SERIES_PRIMARY_IMAGE,
					),
					imageTypeLimit = 1,
					parentId = item.id,
					groupItems = true,
					limit = ITEM_LIMIT,
				)

				val title = context.getString(R.string.lbl_latest_in, item.name)
				HomeFragmentBrowseRowDefRow(BrowseRowDef(title, request, arrayOf(ChangeTriggerType.LibraryUpdated)))
			}.forEach { row ->
				// Add row to adapter
				row.addToRowsAdapter(context, cardPresenter, rowsAdapter)
			}
	}

	companion object {
		// Collections excluded from latest row based on app support and common sense
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
