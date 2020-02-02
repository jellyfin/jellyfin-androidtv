package org.jellyfin.androidtv.details

import androidx.leanback.widget.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.presentation.InfoCardPresenter


class MovieDetailsFragment(item: Movie) : BaseDetailsFragment<Movie>(item) {
	private val detailsRow by lazy { DetailsOverviewRow("") }
	private val chaptersRow by lazy { Row() }
	private val staffRow by lazy { Row() }
	private val charactersRow by lazy { Row() }
	private val relatedRow by lazy { Row() }
	private val mediaInfoRow by lazy { ListRow(HeaderItem("Media info"), ArrayObjectAdapter(InfoCardPresenter())) }

	override fun onCreateAdapter(adapter: ArrayObjectAdapter, selector: ClassPresenterSelector) {
		super.onCreateAdapter(adapter, selector)

		// Add presenters
		selector.addClassPresenter(ListRow::class.java, ListRowPresenter())

		// Add rows
		adapter.add(detailsRow)
//		adapter.add(chaptersRow)
//		adapter.add(staffRow)
//		adapter.add(charactersRow)
//		adapter.add(relatedRow)
		adapter.add(mediaInfoRow)
	}

	override suspend fun setItem(item: Movie) {
		super.setItem(item)

		// Update detail row
		detailsRow.item = item
		detailsRow.actionsAdapter = ArrayObjectAdapter().apply {
			//			if (item.canResume) add(ResumeAction(context!!, item))
//			add(PlayFromBeginningAction(context!!, item))
//			add(ToggleWatchedAction(context!!, item))

			add(Action(0, "Resume").apply { icon = context!!.getDrawable(R.drawable.ic_resume) }) // Resume watching
			add(Action(0, "Play").apply { icon = context!!.getDrawable(R.drawable.ic_play) }) // Play from beginning
			add(Action(0, "Watched").apply { icon = context!!.getDrawable(R.drawable.ic_watch) }) // Set watch state (toggle)
			add(Action(0, "Favorite").apply { icon = context!!.getDrawable(R.drawable.ic_heart) }) // Favorite item (toggle)
			add(Action(0, "More").apply { icon = context!!.getDrawable(R.drawable.lb_ic_more) }) // Show menu with more options
		}

		detailsRow.setImageBitmap(context!!, item.images.primary?.getBitmap(context!!))

		// Update media info data
		mediaInfoRow.adapter.also {
			it as ArrayObjectAdapter

			it.clear()
			item.mediaInfo.streams.forEach(it::add)
		}
	}
}
