package org.jellyfin.androidtv.details.fragments

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.details.actions.DeleteAction
import org.jellyfin.androidtv.details.actions.PlayFromBeginningAction
import org.jellyfin.androidtv.details.actions.SecondariesPopupAction
import org.jellyfin.androidtv.details.actions.ToggleFavoriteAction
import org.jellyfin.androidtv.details.presenters.ChapterInfoPresenter
import org.jellyfin.androidtv.details.presenters.ItemPresenter
import org.jellyfin.androidtv.details.presenters.PersonPresenter
import org.jellyfin.androidtv.details.rows.DetailsOverviewRow
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.model.itemtypes.Series
import org.jellyfin.androidtv.util.addIfNotEmpty
import org.jellyfin.androidtv.util.apiclient.getSimilarItems
import org.jellyfin.androidtv.util.dp

class SeriesDetailsFragment(private val series: Series) : BaseDetailsFragment<Series>(series) {
	// Action definitions
	private val actions by lazy {
		//todo
		val item = MutableLiveData(series)

		listOf(
//			ResumeAction(requireContext(), item),
			PlayFromBeginningAction(requireContext(), item),
//			ToggleWatchedAction(requireContext(), item),
			ToggleFavoriteAction(requireContext(), item),

			// "More" button
			SecondariesPopupAction(requireContext(), listOf(
				DeleteAction(requireContext(), item) { activity?.finish() }
			))
		)
	}

	// Row definitions
	private val detailRow by lazy { DetailsOverviewRow(series, actions, series.images.primary, series.images.backdrops) }
	private val seasonsRow by lazy { createListRow("Seasons", emptyList(), ChapterInfoPresenter(requireContext())) }
	private val castRow by lazy { createListRow("Cast/Crew", series.cast, PersonPresenter(requireContext())) }
	private val relatedItemsRow by lazy { createListRow("Similar", emptyList(), ItemPresenter(requireContext(), 100.dp, 150.dp, false)) }

	override suspend fun onCreateAdapters(rowSelector: ClassPresenterSelector, rowAdapter: ArrayObjectAdapter) {
		super.onCreateAdapters(rowSelector, rowAdapter)

		// Retrieve additional info
		loadAdditionalInformation()

		// Add rows
		rowAdapter.apply {
			add(detailRow)
			addIfNotEmpty(seasonsRow)
			addIfNotEmpty(castRow)
			addIfNotEmpty(relatedItemsRow)
		}
	}

	private suspend fun loadAdditionalInformation() = withContext(Dispatchers.IO) {
		// Get additional information asynchronously
		awaitAll(
				//TODO
//			async {
//				val seasons = TvApp.getApplication().apiClient.getSeasons(series).orEmpty()
//				(seasonsRow.adapter as ArrayObjectAdapter).apply { seasons.forEach(::add) }
//			},
			async {
				//todo filter on server side?
				val relatedItems = TvApp.getApplication().apiClient.getSimilarItems(series).orEmpty().filterIsInstance<Movie>()
				(relatedItemsRow.adapter as ArrayObjectAdapter).apply { relatedItems.forEach(::add) }
			}
		)
	}
}
