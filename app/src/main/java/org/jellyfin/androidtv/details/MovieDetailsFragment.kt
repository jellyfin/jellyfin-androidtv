package org.jellyfin.androidtv.details

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.details.actions.BaseAction
import org.jellyfin.androidtv.details.actions.PlayFromBeginningAction
import org.jellyfin.androidtv.details.actions.ResumeAction
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.presentation.InfoCardPresenter

private const val LOG_TAG = "MovieDetailsFragment"

class MovieDetailsFragment(private val movie: Movie) : BaseDetailsFragment(movie) {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		addRows()
	}

	private fun addRows() = GlobalScope.launch(Dispatchers.Main) {
		Log.i(LOG_TAG, movie.name)

		val primaryImageBitmap = withContext(Dispatchers.IO) {
			movie.images.primary?.getBitmap(context!!)
		}

		adapter = ArrayObjectAdapter(ClassPresenterSelector().apply {
			// Add details presenter
			addClassPresenter(DetailsOverviewRow::class.java, FullWidthDetailsOverviewRowPresenter(
				DetailsDescriptionPresenter(),
				DetailsOverviewLogoPresenter()
			).apply {
				onActionClickedListener = OnActionClickedListener { action ->
					if (action is BaseAction) action.onClick()
					else Log.e(LOG_TAG, "The clicked action did not derive from BaseAction")
				}
			})

			// Add list presenter
			addClassPresenter(ListRow::class.java, ListRowPresenter())
		}).apply {
			// Add details row
			add(DetailsOverviewRow(movie).apply {
				setImageBitmap(context, primaryImageBitmap)

				actionsAdapter = ArrayObjectAdapter().apply {
					if (movie.canResume) add(ResumeAction(context!!, movie))
					add(PlayFromBeginningAction(context!!, movie))

					// todo
					add(Action(1, "Set Watched"))
					add(Action(1, "Add Favorite"))
					add(Action(1, "Add to Queue"))
					add(Action(1, "Go to Series"))
					add(Action(1, "Delete"))
				}
			})

			// todo Chapters/Scenes
			// todo Cast
			// todo Related content / Recommendations

			// Add a Related items row
			add(ListRow(
				HeaderItem(0, "Related Items"),
				ArrayObjectAdapter(StringPresenter()).apply {
					add("Media Item 1")
					add("Media Item 2")
					add("Media Item 3")
				}
			))

			add(ListRow(
				HeaderItem("Media info"),
				ArrayObjectAdapter(InfoCardPresenter()).apply {
					movie.mediaInfo.streams.forEach(::add)
				}
			))
		}
	}
}

class StringPresenter : Presenter() {
	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		return ViewHolder(TextView(parent.context))
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
		(viewHolder.view as TextView).text = item as String
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder?) {

	}
}

