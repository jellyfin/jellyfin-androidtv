package org.jellyfin.androidtv.ui.search

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.leanback.app.SearchSupportFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LeanbackSearchFragment :
	SearchSupportFragment(),
	SearchSupportFragment.SearchResultProvider {

	private val viewModel by viewModels<SearchViewModel>()

	private val searchFragmentDelegate = SearchFragmentDelegate(this)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setSearchResultProvider(this)
		setOnItemViewClickedListener(
			searchFragmentDelegate.onItemViewClickedListener
		)
		setOnItemViewSelectedListener(
			searchFragmentDelegate.onItemViewSelectedListener
		)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		observeSearchResults()
	}

	private fun observeSearchResults() {
		viewModel.searchResultsFlow
			.onEach {
				searchFragmentDelegate.showResults(it)
			}.launchIn(lifecycleScope)
	}

	override fun getResultsAdapter() = searchFragmentDelegate.rowsAdapter

	override fun onQueryTextChange(query: String): Boolean =
		viewModel.searchDebounced(query)

	override fun onQueryTextSubmit(query: String): Boolean =
		viewModel.search(query)

}
