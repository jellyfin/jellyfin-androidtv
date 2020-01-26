package org.jellyfin.androidtv.details

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.ArrayObjectAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.util.randomOrNull

open class BaseDetailsFragment(private val item: BaseItem) : DetailsSupportFragment() {
	private val backgroundController by lazy { DetailsSupportFragmentBackgroundController(this) }
	protected lateinit var rowsAdapter: ArrayObjectAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		addLogo()
		addBackground()
	}

	private fun addLogo() = GlobalScope.launch(Dispatchers.Main) {
		// Logo
		item.images.logo?.getBitmap(context!!)?.let {
			badgeDrawable = BitmapDrawable(resources, it)
		}
	}

	private fun addBackground() = GlobalScope.launch(Dispatchers.Main) {
		val image = item.images.backdrops.randomOrNull() ?: return@launch

		backgroundController.apply {
			enableParallax()
			coverBitmap = image.getBitmap(context!!)
		}
	}
}
