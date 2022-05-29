package org.jellyfin.androidtv.ui.search

import android.content.Context
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ObjectAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.presentation.CustomListRowPresenter
import kotlin.time.Duration.Companion.milliseconds

class SearchProvider(
	context: Context,
	private val lifecycle: Lifecycle,
) : SearchSupportFragment.SearchResultProvider {
	companion object {
		private val SEARCH_DELAY = 600.milliseconds
	}

	private val rowsAdapter = ArrayObjectAdapter(CustomListRowPresenter())
	private var previousQuery: String? = null
	private val searchRunnable = SearchRunnable(context, rowsAdapter)
	private var searchJob: Job? = null

	override fun getResultsAdapter(): ObjectAdapter = rowsAdapter

	override fun onQueryTextChange(query: String): Boolean = search(query, true)
	override fun onQueryTextSubmit(query: String): Boolean = search(query, false)

	private fun search(query: String, delayed: Boolean): Boolean {
		if (query.isBlank()) {
			rowsAdapter.clear()
			return true
		}

		// Don't search the same thing twice
		if (query == previousQuery) return false
		previousQuery = query

		searchJob?.cancel()
		searchJob = lifecycle.coroutineScope.launch {
			if (delayed) delay(SEARCH_DELAY)
			searchRunnable.run(query)
		}

		return true
	}
}
