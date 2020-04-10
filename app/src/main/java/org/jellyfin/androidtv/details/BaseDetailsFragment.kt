package org.jellyfin.androidtv.details

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.base.IItemClickListener
import org.jellyfin.androidtv.model.itemtypes.BaseItem

abstract class BaseDetailsFragment<T : BaseItem>(private val initialItem: T) : RowsSupportFragment(), OnItemViewClickedListener {
	protected val rowSelector by lazy { ClassPresenterSelector() }
	protected val rowAdapter by lazy { ArrayObjectAdapter(rowSelector) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		GlobalScope.launch(Dispatchers.Main) {
			// Add rows and actions
			onCreateAdapters(rowSelector, rowAdapter)

			// Set the Leanback adapter to our own adapter
			adapter = rowAdapter

			// Use our own click listener
			onItemViewClickedListener = this@BaseDetailsFragment
		}
	}

	@CallSuper
	open suspend fun onCreateAdapters(rowSelector: ClassPresenterSelector, rowAdapter: ArrayObjectAdapter) {
		// Add the most used presenters to prevent duplicate code
		rowSelector.addClassPresenter(DetailsOverviewRow::class.java, DetailsOverviewPresenter(context!!))
		rowSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
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
