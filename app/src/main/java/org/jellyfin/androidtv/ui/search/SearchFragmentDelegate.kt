package org.jellyfin.androidtv.ui.search

import androidx.fragment.app.Fragment
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.CustomListRowPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SearchFragmentDelegate(
	private val fragment: Fragment
) : KoinComponent {

	private val backgroundService: BackgroundService by inject()

	val rowsAdapter = MutableObjectAdapter<Row>(CustomListRowPresenter())

	fun showResults(searchResultGroups: List<SearchResultGroup>) {
		rowsAdapter.clear()
		val adapters = mutableListOf<ItemRowAdapter>()
		for ((labelRes, baseItems) in searchResultGroups) {
			val adapter = ItemRowAdapter(fragment.requireContext(), baseItems, CardPresenter(), rowsAdapter, QueryType.Search).apply {
				setRow(ListRow(HeaderItem(fragment.requireContext().getString(labelRes)), this))
			}
			adapters.add(adapter)
		}
		for (adapter in adapters) adapter.Retrieve()
	}

	val onItemViewClickedListener =
		OnItemViewClickedListener { _, item, _, row ->
			if (item !is BaseRowItem) return@OnItemViewClickedListener
			row as ListRow
			val adapter = row.adapter as ItemRowAdapter
			ItemLauncher.launch(item as BaseRowItem?, adapter, item.index, fragment.requireActivity())
		}

	val onItemViewSelectedListener = OnItemViewSelectedListener { _, item, _, _ ->
		val searchHint = item?.let { (item as BaseRowItem).searchHint }
		if (searchHint != null) {
			backgroundService.setBackground(searchHint)
		} else {
			backgroundService.clearBackgrounds()
		}
	}
}
