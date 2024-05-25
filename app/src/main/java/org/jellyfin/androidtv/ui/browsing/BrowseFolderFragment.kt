package org.jellyfin.androidtv.ui.browsing

import android.os.Bundle
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.constant.Extras
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.android.ext.android.inject

abstract class BrowseFolderFragment : BrowseSupportFragment(), RowLoader {
	protected var folder: BaseItemDto? = null
	protected var includeType: String? = null

	protected var rows = mutableListOf<BrowseRowDef>()

	private val backgroundService by inject<BackgroundService>()
	private val itemLauncher by inject<ItemLauncher>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Parse intent
		folder = Json.decodeFromString<BaseItemDto>(arguments?.getString(Extras.Folder)!!)
		includeType = arguments?.getString(Extras.IncludeType)

		// Set BrowseSupportFragment properties
		title = folder?.name
		headersState = HEADERS_DISABLED

		// Add event listeners
		onItemViewClickedListener = OnItemViewClickedListener { _: Presenter.ViewHolder?, item: Any?, _: RowPresenter.ViewHolder?, row: Row ->
			if (item is BaseRowItem) {
				itemLauncher.launch(
					item,
					(row as ListRow).adapter as ItemRowAdapter,
					requireContext()
				)
			}
		}
		onItemViewSelectedListener = OnItemViewSelectedListener { _: Presenter.ViewHolder?, item: Any?, _: RowPresenter.ViewHolder?, row: Row ->
			if (item !is BaseRowItem) {
				backgroundService.clearBackgrounds()
			} else {
				val adapter = (row as? ListRow)?.adapter
				if (adapter is ItemRowAdapter) adapter.loadMoreItemsIfNeeded(adapter.indexOf(item))

				backgroundService.setBackground(item.baseItem)
			}
		}

		// Initialize
		lifecycleScope.launch {
			lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
				setupQueries(this@BrowseFolderFragment)
			}
		}
	}

	protected abstract suspend fun setupQueries(rowLoader: RowLoader)

	override fun loadRows(rows: MutableList<BrowseRowDef>) {
		val mutableAdapter = MutableObjectAdapter<Row>(PositionableListRowPresenter()).also {
			adapter = it
		}

		val cardPresenter = CardPresenter()

		for (def in rows) {
			ItemRowAdapter(
				requireContext(),
				def.query,
				def.chunkSize,
				def.preferParentThumb,
				def.isStaticHeight,
				cardPresenter,
				mutableAdapter,
				def.queryType
			).apply {
				val row = ListRow(HeaderItem(def.headerText), this)
				setReRetrieveTriggers(def.changeTriggers)
				setRow(row)
				Retrieve()
				mutableAdapter.add(row)
			}
		}
	}
}
