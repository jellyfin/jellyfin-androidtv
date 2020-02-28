package org.jellyfin.androidtv.details

import androidx.leanback.widget.*
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.details.actions.*
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.presentation.InfoCardPresenter
import org.jellyfin.androidtv.util.apiclient.getLocalTrailers
import org.jellyfin.androidtv.util.apiclient.getSimilarItems
import org.jellyfin.androidtv.util.apiclient.getSpecialFeatures
import org.jellyfin.androidtv.util.dp

private const val LOG_TAG = "MovieDetailsFragment"

class MovieDetailsFragment(item: Movie) : BaseDetailsFragment<Movie>(item) {

	private val detailsRow by lazy { DetailsOverviewRow(Unit).apply { actionsAdapter = ActionAdapter() } }
	private val chaptersRow by lazy { ListRow(HeaderItem("Chapters"), ArrayObjectAdapter(ChapterInfoPresenter(this.context!!))) }
	private val specialsRow by lazy { ListRow(HeaderItem("Specials"), ArrayObjectAdapter(ItemPresenter(this.context!!, 250.dp, 140.dp, false))) }
	private val charactersRow by lazy { ListRow(HeaderItem("Cast/Crew"), ArrayObjectAdapter(PersonPresenter(this.context!!))) }
	private val similarsRow by lazy { ListRow(HeaderItem("Similar"), ArrayObjectAdapter(ItemPresenter(this.context!!, 100.dp, 150.dp, false))) }
	private val localTrailersRow by lazy { ListRow(HeaderItem("Trailers"), ArrayObjectAdapter(ItemPresenter(this.context!!, 250.dp, 140.dp, false))) }
	private val mediaInfoRow by lazy { ListRow(HeaderItem("Media info"), ArrayObjectAdapter(InfoCardPresenter())) }

	override fun onCreateAdapter(adapter: StateObjectAdapter<Row>, selector: ClassPresenterSelector) {
		super.onCreateAdapter(adapter, selector)

		// Add presenters
		selector.addClassPresenter(ListRow::class.java, ListRowPresenter())

		// Add rows
		adapter.add(detailsRow)
		adapter.add(chaptersRow)
		adapter.add(specialsRow)
		adapter.add(charactersRow)
		adapter.add(similarsRow)
		adapter.add(localTrailersRow)
		adapter.add(mediaInfoRow)
	}

	override suspend fun setItem(item: Movie) {
		super.setItem(item)

		// Update detail row
		detailsRow.item = item

		// Update actions
		(detailsRow.actionsAdapter as ActionAdapter).apply {
			reset()

			if (item.canResume) add(ResumeAction(context!!, item))
			add(PlayFromBeginningAction(context!!, item))
			add(ToggleWatchedAction(context!!, item))
			add(ToggleFavoriteAction(context!!, item))
			// TODO: Bring this back once we have a more understandable queue implementation for users
			//add(AddToQueueAction(context!!, item))

			// Menu with more actions
			add(SecondariesPopupAction(context!!).apply {
				add(DeleteAction(context!!, item) { activity?.finish() }).apply {
					isVisible = TvApp.getApplication().currentUser.policy.enableContentDeletion
				}
			})

			commit()
		}

		detailsRow.setImageBitmap(context!!, item.images.primary?.getBitmap(context!!))

		//todo hacky way to get the adapter..
		val adapter = adapter as StateObjectAdapter<Row>
		val specials = TvApp.getApplication().apiClient.getSpecialFeatures(item).orEmpty()
		adapter.setVisibility(specialsRow, specials.isNotEmpty())
		specialsRow.adapter.also {
			it as ArrayObjectAdapter
			it.clear()
			specials.forEach(it::add)
		}

		adapter.setVisibility(chaptersRow, item.chapters.isNotEmpty())
		chaptersRow.adapter.also {
			it as ArrayObjectAdapter
			it.clear()
			item.chapters.forEach(it::add)
		}

		adapter.setVisibility(charactersRow, item.cast.isNotEmpty())
		charactersRow.adapter.also {
			it as ArrayObjectAdapter
			it.clear()
			item.cast.forEach(it::add)
		}

		val similarMovies = TvApp.getApplication().apiClient.getSimilarItems(item).orEmpty().filterIsInstance<Movie>()
		adapter.setVisibility(similarsRow, similarMovies.isNotEmpty())
		similarsRow.adapter.also {
			it as ArrayObjectAdapter
			it.clear()
			similarMovies.forEach(it::add)
		}

		val localTrailers = TvApp.getApplication().apiClient.getLocalTrailers(item).orEmpty()
		adapter.setVisibility(localTrailersRow, localTrailers.isNotEmpty())
		localTrailersRow.adapter.also {
			it as ArrayObjectAdapter
			it.clear()
			localTrailers.forEach(it::add)
		}

		// Update media info data
		adapter.setVisibility(mediaInfoRow, TvApp.getApplication().userPreferences.debuggingEnabled)
		mediaInfoRow.adapter.also {
			it as ArrayObjectAdapter
			it.clear()
			item.mediaInfo.streams.forEach(it::add)
		}
	}
}
