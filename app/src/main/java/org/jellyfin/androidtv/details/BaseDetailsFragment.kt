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
import org.jellyfin.androidtv.details.actions.BaseAction
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.util.randomOrNull

abstract class BaseDetailsFragment<T : BaseItem>(private val initialItem: T) : DetailsSupportFragment() {
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
			val adapter = ArrayObjectAdapter(selector)
			onCreateAdapter(adapter, selector)

			// Set item values (todo make everything suspended?)
			setItem(initialItem)
			initialItem.addChangeListener(::changeListener)

			// Set adapter
			this@BaseDetailsFragment.adapter = adapter
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
	protected open fun onCreateAdapter(adapter: ArrayObjectAdapter, selector: ClassPresenterSelector) {
		selector.addClassPresenter(DetailsOverviewRow::class.java, FullWidthDetailsOverviewRowPresenter(
			DetailsDescriptionPresenter(),
			DetailsOverviewLogoPresenter()
		).apply {
			setOnActionClickedListener { action ->
				if (action is BaseAction) action.onClick()
			}
		})
	}

	@CallSuper
	open suspend fun setItem(item: T) {
		// Logo
		badgeDrawable = item.images.logo?.getBitmap(context!!)?.let { BitmapDrawable(resources, it) }

		// Background
		//todo: Use all backgrounds with transition (fade/slide)
		val backgroundImage = item.images.backdrops.randomOrNull()

		backgroundController.apply {
			coverBitmap = backgroundImage?.getBitmap(context!!)
		}
	}
}
