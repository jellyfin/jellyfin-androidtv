package org.jellyfin.androidtv.details

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.leanback.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.Episode
import org.jellyfin.apiclient.model.dto.ImageOptions

class EpisodeDetailsFragment(private val data: Episode) : BaseDetailsFragment() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)


		GlobalScope.launch(Dispatchers.Main) {
			buildDetails()
		}
	}

	private suspend fun buildDetails() {

		Log.i("EpisodeDetailsFragment", data.name)

		val primaryImageUrl = TvApp.getApplication().apiClient.GetImageUrl(data.id, ImageOptions())
		val primaryImageBitmap = getImageFromURL(primaryImageUrl)

		val selector = ClassPresenterSelector().apply {
			// Attach your media item details presenter to the row presenter:
			val detailsDescriptionPresenter = DetailsDescriptionPresenter(title = data.name, subtitle = "TODO", body = data.description)


			addClassPresenter(DetailsOverviewRow::class.java,
				FullWidthDetailsOverviewRowPresenter(
					detailsDescriptionPresenter,
					primaryImageBitmap?.let { DetailsOverviewLogoPresenter() }))


			addClassPresenter(ListRow::class.java, ListRowPresenter())
		}
		rowsAdapter = ArrayObjectAdapter(selector)

		val detailsOverview = DetailsOverviewRow("Media Item Details").apply {
			imageDrawable = BitmapDrawable(resources, primaryImageBitmap)
			// Add images and action buttons to the details view
			addAction(Action(1, "Continue from 12:10"))
			addAction(Action(2, "Play from Beginning"))
		}
		rowsAdapter.add(detailsOverview)

		// Add a Related items row
//		val listRowAdapter = ArrayObjectAdapter(StringPresenter()).apply {
//			add("Media Item 1")
//			add("Media Item 2")
//			add("Media Item 3")
//		}
//		val header = HeaderItem(0, "Related Items")
//		rowsAdapter.add(ListRow(header, listRowAdapter))

		adapter = rowsAdapter
	}
}
