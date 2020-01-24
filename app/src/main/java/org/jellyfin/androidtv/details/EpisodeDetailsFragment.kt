package org.jellyfin.androidtv.details

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.leanback.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.details.actions.BaseAction
import org.jellyfin.androidtv.details.actions.PlayFromBeginningAction
import org.jellyfin.androidtv.details.actions.ResumeAction
import org.jellyfin.androidtv.model.itemtypes.Episode
import org.jellyfin.apiclient.model.dto.ImageOptions

private const val LOG_TAG = "EpisodeDetailsFragment"

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

			val overviewRowPresenter = FullWidthDetailsOverviewRowPresenter(
				detailsDescriptionPresenter,
				primaryImageBitmap?.let { DetailsOverviewLogoPresenter() })

			overviewRowPresenter.onActionClickedListener = OnActionClickedListener {
				val action = it as BaseAction
				action.onClick()
			}

			addClassPresenter(DetailsOverviewRow::class.java, overviewRowPresenter)


			addClassPresenter(ListRow::class.java, ListRowPresenter())
		}
		rowsAdapter = ArrayObjectAdapter(selector)

		val actionsAdapter = SparseArrayObjectAdapter()
		if (data.canResume) actionsAdapter.set(0, ResumeAction(context!!, data.playbackPositionTicks, data.id))
		actionsAdapter.set(1, PlayFromBeginningAction(context!!, data.id))
		actionsAdapter.set(2, Action(1, "Set Watched"))
		actionsAdapter.set(3, Action(1, "Add Favorite"))
		actionsAdapter.set(4, Action(1, "Add to Queue"))
		actionsAdapter.set(5, Action(1, "Go to Series"))
		actionsAdapter.set(6, Action(1, "Delete"))

		val detailsOverview = DetailsOverviewRow("Media Item Details").also {
			it.imageDrawable = BitmapDrawable(resources, primaryImageBitmap)
			it.actionsAdapter = actionsAdapter
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
