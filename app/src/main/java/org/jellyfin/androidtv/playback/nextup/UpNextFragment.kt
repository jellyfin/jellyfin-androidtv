package org.jellyfin.androidtv.playback.nextup

import android.os.Bundle
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DetailsOverviewRow
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.apiclient.model.dto.ImageOptions

class UpNextFragment : DetailsSupportFragment() {
	private val background = DetailsSupportFragmentBackgroundController(this)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		GlobalScope.launch(Dispatchers.Main) {
			val item = withContext(Dispatchers.IO) {
				//todo currently hardcoded for testing
//				TvApp.getApplication().apiClient.getItem("e4f090de63eaf5fb5d34df6a7f8e504e")
				TvApp.getApplication().apiClient.getItem("aa170e9f519e71d17724f1c8a045c027")
			}

			if (item == null) return@launch

			background.enableParallax()
			val backdropUrls = TvApp.getApplication().apiClient.GetBackdropImageUrls(item, ImageOptions())

			if (backdropUrls.isNotEmpty()) {
				val bitmap = withContext(Dispatchers.IO) { Picasso.with(activity).load(backdropUrls.first()).get() }
				background.coverBitmap = bitmap
			}

			// Setup PresenterSelector to distinguish between the different rows.
			val rowPresenterSelector = ClassPresenterSelector()
			rowPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, FullWidthDetailsOverviewRowPresenter(UpNextDetailsPresenter(activity!!)))
			val mRowsAdapter = ArrayObjectAdapter(rowPresenterSelector)

			// Setup action and detail row.
			val thumbnail = TvApp.getApplication().apiClient.GetImageUrl(item, ImageOptions())
			mRowsAdapter.add(DetailsOverviewRow(UpNextItemData(item.id, thumbnail, item.name, item.overview)))

			adapter = mRowsAdapter
		}
	}
}
