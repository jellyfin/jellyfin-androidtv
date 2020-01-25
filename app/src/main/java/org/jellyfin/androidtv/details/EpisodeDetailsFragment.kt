package org.jellyfin.androidtv.details

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.leanback.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.details.actions.BaseAction
import org.jellyfin.androidtv.details.actions.PlayFromBeginningAction
import org.jellyfin.androidtv.details.actions.ResumeAction
import org.jellyfin.androidtv.model.itemtypes.Episode

private const val LOG_TAG = "EpisodeDetailsFragment"

class EpisodeDetailsFragment(private val episode: Episode) : BaseDetailsFragment(episode) {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)


		GlobalScope.launch(Dispatchers.Main) {
			buildDetails()
		}
	}

	private suspend fun buildDetails() {

		Log.i("EpisodeDetailsFragment", episode.name)

		val primaryImageBitmap = withContext(Dispatchers.IO) {
			episode.images.primary?.getBitmap(context!!)
		}

		val selector = ClassPresenterSelector().apply {
			// Attach your media item details presenter to the row presenter:
			val detailsDescriptionPresenter = DetailsDescriptionPresenter(title = episode.name, subtitle = "TODO", body = episode.description)

			val overviewRowPresenter = FullWidthDetailsOverviewRowPresenter(
				detailsDescriptionPresenter,
				primaryImageBitmap?.let { DetailsOverviewLogoPresenter() })

			overviewRowPresenter.onActionClickedListener = OnActionClickedListener {
				if (it is BaseAction) {
					val action = it as BaseAction
					action.onClick()
				} else {
					Log.e(LOG_TAG, "The clicked Action did not derive from BaseAction, this is unsupported!")
				}
			}

			addClassPresenter(DetailsOverviewRow::class.java, overviewRowPresenter)
			addClassPresenter(ListRow::class.java, ListRowPresenter())
		}
		rowsAdapter = ArrayObjectAdapter(selector)

		val actionsAdapter = ArrayObjectAdapter().apply {
			if (episode.canResume) add(ResumeAction(context!!, episode))
			add(PlayFromBeginningAction(context!!, episode))
			add(Action(1, "Set Watched"))
			add(Action(1, "Add Favorite"))
			add(Action(1, "Add to Queue"))
			add(Action(1, "Go to Series"))
			add(Action(1, "Delete"))
		}

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
