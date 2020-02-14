package org.jellyfin.androidtv.details

import androidx.leanback.widget.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.details.actions.*
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.presentation.InfoCardPresenter
import org.jellyfin.androidtv.util.apiclient.getSimilarItems
import org.jellyfin.androidtv.util.apiclient.getSpecialFeatures
import org.jellyfin.androidtv.util.dp

private const val LOG_TAG = "MovieDetailsFragment"

class MovieDetailsFragment(item: Movie) : BaseDetailsFragment<Movie>(item) {

	private val detailsRow by lazy { DetailsOverviewRow(Unit).apply { actionsAdapter = ActionAdapter() } }
	private val chaptersRow by lazy { ListRow(HeaderItem("Chapters"), ArrayObjectAdapter(ChapterInfoPresenter(this.context!!))) }
	private val specialsRow by lazy { ListRow(HeaderItem("Specials"), ArrayObjectAdapter(ItemPresenter(this.context!!, 250.dp, 140.dp,false))) }
//	private val staffRow by lazy { Row() }
	private val charactersRow by lazy { ListRow(HeaderItem("Cast/Crew"), ArrayObjectAdapter(PersonPresenter(this.context!!))) }
	private val similarsRow by lazy { ListRow(HeaderItem("Similar"), ArrayObjectAdapter(ItemPresenter(this.context!!, 100.dp, 150.dp, false))) }
	private val mediaInfoRow by lazy { ListRow(HeaderItem("Media info"), ArrayObjectAdapter(InfoCardPresenter())) }

	override fun onCreateAdapter(adapter: ArrayObjectAdapter, selector: ClassPresenterSelector) {
		super.onCreateAdapter(adapter, selector)

		// Add presenters
		selector.addClassPresenter(ListRow::class.java, ListRowPresenter())

		// Add rows
		adapter.add(detailsRow)
		adapter.add(chaptersRow)
		adapter.add(specialsRow)
//		adapter.add(staffRow)
		adapter.add(charactersRow)
		adapter.add(similarsRow)
		adapter.add(mediaInfoRow)
	}

	override suspend fun setItem(item: Movie) {
		super.setItem(item)

		// Update detail row
		detailsRow.item = item

		// Update actions
		(detailsRow.actionsAdapter as ActionAdapter).apply {
			reset()

			if (item.canResume) add(ResumeAction(context!!, item).apply { icon = context!!.getDrawable(R.drawable.ic_resume) })

			add(PlayFromBeginningAction(context!!, item).apply { icon = context!!.getDrawable(R.drawable.ic_play) })
			add(ToggleWatchedAction(context!!, item).apply { icon = context!!.getDrawable(R.drawable.ic_watch) })
			add(ToggleFavoriteAction(context!!, item).apply { icon = context!!.getDrawable(R.drawable.ic_heart) })
			add(Action(0, "More").apply { icon = context!!.getDrawable(R.drawable.ic_more) }) // Show menu with more options

			commit()
		}

		detailsRow.setImageBitmap(context!!, item.images.primary?.getBitmap(context!!))

		val specials = TvApp.getApplication().apiClient.getSpecialFeatures(item)
		specialsRow.adapter.also {
			it as ArrayObjectAdapter
			it.clear()
			specials?.forEach(it::add)
		}

		chaptersRow.adapter.also {
			it as ArrayObjectAdapter
			it.clear()
			item.chapters.forEach(it::add)
		}

		charactersRow.adapter.also {
			it as ArrayObjectAdapter
			it.clear()
			item.cast.forEach(it::add)
		}

		val similarMovies = TvApp.getApplication().apiClient.getSimilarItems(item)?.filterIsInstance<Movie>()
		similarsRow.adapter.also {
			it as ArrayObjectAdapter
			it.clear()
			similarMovies?.forEach(it::add)
		}

		// Update media info data
		mediaInfoRow.adapter.also {
			it as ArrayObjectAdapter
			it.clear()
			item.mediaInfo.streams.forEach(it::add)
		}
	}
}
