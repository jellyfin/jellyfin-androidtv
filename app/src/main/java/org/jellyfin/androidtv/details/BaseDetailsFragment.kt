package org.jellyfin.androidtv.details

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.base.IItemClickListener
import org.jellyfin.androidtv.details.actions.ActionAdapter
import org.jellyfin.androidtv.details.actions.BaseAction
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.util.randomOrNull

abstract class BaseDetailsFragment<T : BaseItem>(private val initialItem: T) : DetailsSupportFragment(), OnItemViewClickedListener {
	private val backgroundController by lazy {
		DetailsSupportFragmentBackgroundController(this).apply {
			enableParallax()
		}
	}

	protected val rowSelector by lazy { ClassPresenterSelector() }
	protected val rowAdapter by lazy { ArrayObjectAdapter(rowSelector) }
	protected val actionAdapter by lazy { ActionAdapter() }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		GlobalScope.launch(Dispatchers.Main) {
			// Add rows and actions
			onCreateAdapters(rowSelector, rowAdapter, actionAdapter)

			// Set the Leanback adapter to our own adapter
			adapter = rowAdapter

			// Use our own click listener
			onItemViewClickedListener = this@BaseDetailsFragment

			initialItem.images.logo?.load(context!!) { badgeDrawable = BitmapDrawable(resources, it) }
			//todo: Use all backgrounds with transition (fade/slide)
			initialItem.images.backdrops.randomOrNull()?.load(context!!) { backgroundController.coverBitmap = it }
		}
	}

	@CallSuper
	open suspend fun onCreateAdapters(rowSelector: ClassPresenterSelector, rowAdapter: ArrayObjectAdapter, actionAdapter: ActionAdapter) {
		// Add the most used presenters to prevent duplicate code

		rowSelector.addClassPresenter(DetailsOverviewRow::class.java, FullWidthDetailsOverviewRowPresenter(
			DetailsDescriptionPresenter(),
			DetailsOverviewLogoPresenter()
		).apply {
			initialState = FullWidthDetailsOverviewRowPresenter.STATE_HALF

			setOnActionClickedListener { action ->
				if (action is BaseAction) action.onClick()
			}
		})

		rowSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
	}

	override fun onSetDetailsOverviewRowStatus(presenter: FullWidthDetailsOverviewRowPresenter, viewHolder: FullWidthDetailsOverviewRowPresenter.ViewHolder, adapterPosition: Int, selectedPosition: Int, selectedSubPosition: Int) {
		// Force the "initialState" as current state
		presenter.setState(viewHolder, presenter.initialState)
	}

	@CallSuper
	override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
		if (row is ListRow) {
			val presenter = row.adapter.getPresenter(item)
			if (presenter is IItemClickListener) {
				GlobalScope.launch(Dispatchers.Main) {
					presenter.onItemClicked(item)
				}
			}
		}
	}

	// Utility functions
	protected fun createListRow(name: String, items: Iterable<Any>, presenter: Presenter) = ListRow(
		HeaderItem(name),
		ArrayObjectAdapter(presenter).apply {
			items.forEach(::add)
		}
	)
}
