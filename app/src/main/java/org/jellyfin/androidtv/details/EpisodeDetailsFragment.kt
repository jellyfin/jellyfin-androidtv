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
import org.jellyfin.androidtv.details.actions.ToggleWatchedAction
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
			val detailsDescriptionPresenter = DetailsDescriptionPresenter()

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

		val actionsAdapter = SparseArrayObjectAdapter().apply {
			if (episode.canResume) set(0, ResumeAction(context!!, episode))
			set(1, PlayFromBeginningAction(context!!, episode))
			set(2, ToggleWatchedAction(context!!, episode) {
				if (episode.canResume) set(0, ResumeAction(context!!, episode)) else clear(0)
				notifyArrayItemRangeChanged(0,6)
			})
			set(3, Action(1, "Add Favorite"))
			set(4, Action(1, "Add to Queue"))
			set(5, Action(1, "Go to Series"))
			set(6, Action(1, "Delete"))
		}

		val detailsOverview = DetailsOverviewRow(episode).also {
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
