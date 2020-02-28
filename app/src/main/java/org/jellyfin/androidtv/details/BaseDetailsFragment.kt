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
import org.jellyfin.androidtv.details.actions.BaseAction
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.util.randomOrNull

abstract class BaseDetailsFragment<T : BaseItem>(private val initialItem: T) : DetailsSupportFragment(), OnItemViewClickedListener {
	private val backgroundController by lazy {
		DetailsSupportFragmentBackgroundController(this).apply {
			enableParallax()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		GlobalScope.launch(Dispatchers.Main) {
			// Create adapter
			val selector = ClassPresenterSelector()
			val adapter = StateObjectAdapter<Row>(selector)
			onCreateAdapter(adapter, selector)

			// Set adapter
			this@BaseDetailsFragment.adapter = adapter

			// Setup self as item click listener
			this@BaseDetailsFragment.onItemViewClickedListener = this@BaseDetailsFragment

			// Set item values (todo make everything suspended?)
			setItem(initialItem)
			initialItem.addChangeListener(::changeListener)
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		initialItem.removeChangeListener(::changeListener)
	}

	private fun changeListener() {
		GlobalScope.launch(Dispatchers.Main) {
			setItem(initialItem)
		}
	}

	@CallSuper
	protected open fun onCreateAdapter(adapter: StateObjectAdapter<Row>, selector: ClassPresenterSelector) {
		selector.addClassPresenter(DetailsOverviewRow::class.java, FullWidthDetailsOverviewRowPresenter(
			DetailsDescriptionPresenter(),
			DetailsOverviewLogoPresenter()
		).apply {
			initialState = FullWidthDetailsOverviewRowPresenter.STATE_HALF

			setOnActionClickedListener { action ->
				if (action is BaseAction) action.onClick()
			}
		})
	}

	override fun onSetDetailsOverviewRowStatus(presenter: FullWidthDetailsOverviewRowPresenter, viewHolder: FullWidthDetailsOverviewRowPresenter.ViewHolder, adapterPosition: Int, selectedPosition: Int, selectedSubPosition: Int) {
		// Force the "initialState" as current state
		presenter.setState(viewHolder, presenter.initialState)
	}

	@CallSuper
	open suspend fun setItem(item: T) {
		// Logo
		item.images.logo?.load(context!!) { badgeDrawable = BitmapDrawable(resources, it) }

		// Background
		//todo: Use all backgrounds with transition (fade/slide)
		item.images.backdrops.randomOrNull()?.load(context!!) { backgroundController.coverBitmap = it }
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
}
