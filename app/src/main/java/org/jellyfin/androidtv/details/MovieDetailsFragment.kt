package org.jellyfin.androidtv.details

import androidx.leanback.widget.*
import org.jellyfin.androidtv.details.actions.PlayFromBeginningAction
import org.jellyfin.androidtv.details.actions.ResumeAction
import org.jellyfin.androidtv.details.actions.ToggleWatchedAction
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
			if (item.canResume) add(ResumeAction(context!!, item))
			add(PlayFromBeginningAction(context!!, item))
			add(ToggleWatchedAction(context!!, item))

			add(Action(0, "More"))
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

