package org.jellyfin.androidtv.ui.search

import android.os.Bundle
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ListRow
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.koin.java.KoinJavaComponent.inject

class LeanbackSearchFragment : SearchSupportFragment() {
	private val backgroundService = inject<BackgroundService>(BackgroundService::class.java)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Create provider
		val searchProvider = SearchProvider(requireContext(), lifecycle)
		setSearchResultProvider(searchProvider)

		// Add event listeners
		setOnItemViewClickedListener { _, item, _, row ->
			if (item !is BaseRowItem) return@setOnItemViewClickedListener

			val adapter = (row as ListRow).adapter as ItemRowAdapter
			ItemLauncher.launch(item as BaseRowItem?, adapter, item.index, activity)
		}

		setOnItemViewSelectedListener { _, item, _, _ ->
			if (item is BaseRowItem) backgroundService.value.setBackground(item.searchHint!!)
			else backgroundService.value.clearBackgrounds()
		}
	}
}
