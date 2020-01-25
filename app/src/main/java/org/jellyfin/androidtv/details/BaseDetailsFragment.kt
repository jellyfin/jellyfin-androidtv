package org.jellyfin.androidtv.details

import android.os.Bundle
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.ArrayObjectAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.model.itemtypes.BaseItem

open class BaseDetailsFragment(private val item: BaseItem) : DetailsSupportFragment() {
	private val backgroundController = DetailsSupportFragmentBackgroundController(this)
	protected lateinit var rowsAdapter: ArrayObjectAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		addBackground()
	}

	private fun addBackground() = GlobalScope.launch(Dispatchers.Main) {
		val image = item.images.backdrops.firstOrNull() ?: return@launch

		backgroundController.apply {
			enableParallax()
			coverBitmap = image.getBitmap(context!!)
		}
	}
}
