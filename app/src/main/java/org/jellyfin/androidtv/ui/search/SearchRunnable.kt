package org.jellyfin.androidtv.ui.search

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import androidx.lifecycle.Lifecycle
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.apiclient.EmptyLifecycleAwareResponse
import org.jellyfin.apiclient.model.search.SearchQuery
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber

class SearchRunnable(
	private val context: Context,
	private val lifecycle: Lifecycle,
	private val rowsAdapter: MutableObjectAdapter<Row>,
) {
	companion object {
		private const val QUERY_LIMIT = 50
		private val groups = mapOf(
			R.string.lbl_movies to setOf(BaseItemKind.MOVIE, BaseItemKind.BOX_SET),
			R.string.lbl_series to setOf(BaseItemKind.SERIES),
			R.string.lbl_episodes to setOf(BaseItemKind.EPISODE),
			R.string.lbl_people to setOf(BaseItemKind.PERSON),
			R.string.lbl_videos to setOf(BaseItemKind.VIDEO),
			R.string.lbl_recordings to setOf(BaseItemKind.RECORDING),
			R.string.lbl_programs to setOf(BaseItemKind.PROGRAM),
			R.string.lbl_artists to setOf(BaseItemKind.MUSIC_ARTIST),
			R.string.lbl_albums to setOf(BaseItemKind.MUSIC_ALBUM),
			R.string.lbl_songs to setOf(BaseItemKind.AUDIO),
		)
	}

	private val presenter = CardPresenter()

	fun search(searchTerm: String) {
		var responses = 0
		val adapters = mutableListOf<ItemRowAdapter>()

		val finishedListener = object : EmptyLifecycleAwareResponse(lifecycle) {
			override fun onResponse() {
				if (!active) return

				responses++

				if (responses != adapters.size) return

				rowsAdapter.clear()
				for (adapter in adapters) adapter.addToParentIfResultsReceived()
			}

			override fun onError(ex: java.lang.Exception?) {
				if (!active) return

				Timber.e(ex, "Something went wrong while retrieving search results")
				onResponse()
			}
		}

		for ((nameRes, itemTypes) in groups) {
			val query = createSearchQuery(itemTypes, searchTerm)
			val adapter = ItemRowAdapter(context, query, presenter, rowsAdapter)
			adapters.add(adapter)
			adapter.setRow(ListRow(HeaderItem(context.getString(nameRes)), adapter))
			adapter.setRetrieveFinishedListener(finishedListener)
		}

		for (adapter in adapters) adapter.Retrieve()
	}

	private fun createSearchQuery(
		itemTypes: Collection<BaseItemKind>,
		searchTerm: String,
	) = SearchQuery().also { query ->
		query.limit = QUERY_LIMIT
		query.searchTerm = searchTerm
		query.includeItemTypes = itemTypes.map { it.serialName }.toTypedArray()
	}
}
