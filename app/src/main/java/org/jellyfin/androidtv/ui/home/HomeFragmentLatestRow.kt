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
import org.jellyfin.apiclient.model.querying.ItemFields
import org.jellyfin.apiclient.model.querying.LatestItemsQuery
import org.jellyfin.sdk.model.constant.CollectionType
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

class HomeFragmentLatestRow(
	private val userRepository: UserRepository,
	private val userViewsRepository: UserViewsRepository,
) : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		// Get configuration (to find excluded items)
		val configuration = userRepository.currentUser.value?.configuration

		// Create a list of views to include
		val latestItemsExcludes = configuration?.latestItemsExcludes.orEmpty().mapNotNull { it.toUUIDOrNull() }
		val views = runBlocking { userViewsRepository.views.first() }
		views
			.filterNot { item -> item.collectionType in EXCLUDED_COLLECTION_TYPES || item.id in latestItemsExcludes }
			.map { item ->
				// Create query and add it to a new row
				val query = LatestItemsQuery().apply {
					fields = arrayOf(
						ItemFields.PrimaryImageAspectRatio,
						ItemFields.Overview,
						ItemFields.ChildCount,
						ItemFields.SeriesPrimaryImage,
					)
					imageTypeLimit = 1
					parentId = item.id.toString()
					groupItems = true
					limit = ITEM_LIMIT
				}

				val title = context.getString(R.string.lbl_latest_in, item.name)
				HomeFragmentBrowseRowDefRow(BrowseRowDef(title, query, arrayOf(ChangeTriggerType.LibraryUpdated)))
			}.forEach { row ->
				// Add row to adapter
				row.addToRowsAdapter(context, cardPresenter, rowsAdapter)
			}
	}

	companion object {
		// Collections exclused from latest row based on app support and common sense
		private val EXCLUDED_COLLECTION_TYPES = arrayOf(
			CollectionType.Playlists,
			CollectionType.LiveTv,
			CollectionType.BoxSets,
			CollectionType.Books,
		)

		// Maximum ammount of items loaded for a row
		private const val ITEM_LIMIT = 50
	}
}
