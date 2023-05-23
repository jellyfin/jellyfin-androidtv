package org.jellyfin.androidtv.ui.browsing

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
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
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserPreferences.Companion.clockBehavior
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.ui.ClockUserView
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter
import org.jellyfin.androidtv.util.Utils.convertDpToPixel
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.android.ext.android.inject

abstract class BrowseFolderFragment : BrowseSupportFragment(), RowLoader {
	protected var folder: BaseItemDto? = null
	protected var includeType: String? = null

	protected var rows = mutableListOf<BrowseRowDef>()

	private val userPreferences by inject<UserPreferences>()
	private val backgroundService by inject<BackgroundService>()

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
				ItemLauncher.launch(
					item,
					(row as ListRow).adapter as ItemRowAdapter,
					item.index,
					requireContext()
				)
			}
		}
		onItemViewSelectedListener = OnItemViewSelectedListener { _: Presenter.ViewHolder?, item: Any?, _: RowPresenter.ViewHolder?, row: Row ->
			if (item !is BaseRowItem) {
				backgroundService.clearBackgrounds()
			} else {
				val adapter = (row as? ListRow)?.adapter
				if (adapter is ItemRowAdapter) adapter.loadMoreItemsIfNeeded(item.index.toLong())

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

	override fun onStart() {
		super.onStart()

		// Check if clock should be visible
		val showClock = userPreferences[clockBehavior]
		if (showClock !== ClockBehavior.ALWAYS && showClock !== ClockBehavior.IN_MENUS) return

		val root = requireActivity().findViewById<ViewGroup>(androidx.leanback.R.id.browse_frame)

		// Move the title to the left to make way for the clock
		val titleView = root.findViewById<TextView>(androidx.leanback.R.id.title_text)
		if (titleView != null) {
			val layoutParams = titleView.layoutParams as FrameLayout.LayoutParams
			layoutParams.rightMargin = convertDpToPixel(root.context, 120)
			titleView.layoutParams = layoutParams
		}

		// Add the clock element
		val layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
		val clockView = ClockUserView(root.context, null)
		layoutParams.gravity = Gravity.TOP or Gravity.END
		layoutParams.rightMargin = convertDpToPixel(root.context, 40)
		layoutParams.topMargin = convertDpToPixel(root.context, 20)
		clockView.layoutParams = layoutParams
		root.addView(clockView)
	}
}
