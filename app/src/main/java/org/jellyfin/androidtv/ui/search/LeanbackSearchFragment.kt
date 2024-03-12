package org.jellyfin.androidtv.ui.search

import android.os.Bundle
import android.view.View
import androidx.leanback.app.SearchSupportFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class LeanbackSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {
	private val viewModel: SearchViewModel by viewModel()

	private val searchFragmentDelegate: SearchFragmentDelegate by inject {
		parametersOf(requireContext())
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setSearchResultProvider(this)
		setOnItemViewClickedListener(searchFragmentDelegate.onItemViewClickedListener)
		setOnItemViewSelectedListener(searchFragmentDelegate.onItemViewSelectedListener)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		viewModel.searchResultsFlow
			.onEach { searchFragmentDelegate.showResults(it) }
			.launchIn(lifecycleScope)

		val query = arguments?.getString(SearchFragment.EXTRA_QUERY)
		if (!query.isNullOrBlank()) setSearchQuery(query, true)
	}

	override fun getResultsAdapter() = searchFragmentDelegate.rowsAdapter
	override fun onQueryTextChange(query: String): Boolean = viewModel.searchDebounced(query)
	override fun onQueryTextSubmit(query: String): Boolean = viewModel.searchImmediately(query)

}
