package org.jellyfin.androidtv.details.fragments

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.details.DetailsOverviewRow
import org.jellyfin.androidtv.details.actions.*
import org.jellyfin.androidtv.details.presenters.ChapterInfoPresenter
import org.jellyfin.androidtv.details.presenters.ItemPresenter
import org.jellyfin.androidtv.details.presenters.PersonPresenter
import org.jellyfin.androidtv.details.presenters.trailers.ExternalTrailerPresenter
import org.jellyfin.androidtv.details.presenters.trailers.YouTubeTrailerPresenter
import org.jellyfin.androidtv.model.itemtypes.LocalTrailer
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.model.trailers.external.ExternalTrailer
import org.jellyfin.androidtv.model.trailers.external.YouTubeTrailer
import org.jellyfin.androidtv.presentation.InfoCardPresenter
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.addIfNotEmpty
import org.jellyfin.androidtv.util.apiclient.getLocalTrailers
import org.jellyfin.androidtv.util.apiclient.getSimilarItems
import org.jellyfin.androidtv.util.apiclient.getSpecialFeatures
import org.jellyfin.androidtv.util.dp

class MovieDetailsFragment(private val movie: Movie) : BaseDetailsFragment<Movie>(movie) {
	// Action definitions
	private val actions by lazy {
		//todo
		val item = MutableLiveData(movie)

		listOf(
			ResumeAction(requireContext(), item),
			PlayFromBeginningAction(requireContext(), item),
			ToggleWatchedAction(requireContext(), item),
			ToggleFavoriteAction(requireContext(), item),

			// "More" button
			SecondariesPopupAction(requireContext(), listOf(
				DeleteAction(requireContext(), item) { activity?.finish() }
			))
		)
	}

	// Row definitions
	private val detailRow by lazy { DetailsOverviewRow(movie, actions, movie.images.primary, movie.images.backdrops) }
	private val chaptersRow by lazy { createListRow("Chapters", movie.chapters, ChapterInfoPresenter(requireContext())) }
	private val specialsRow by lazy { createListRow("Specials", emptyList(), ItemPresenter(requireContext(), 250.dp, 140.dp, false)) }
	private val castRow by lazy { createListRow("Cast/Crew", movie.cast, PersonPresenter(requireContext())) }
	private val relatedItemsRow by lazy { createListRow("Similar", emptyList(), ItemPresenter(requireContext(), 100.dp, 150.dp, false)) }
	private val trailersRow by lazy { ListRow(HeaderItem("Trailers"),
		ArrayObjectAdapter(
			ClassPresenterSelector().apply{
				addClassPresenter(LocalTrailer::class.java, ItemPresenter(requireContext(), (ImageUtils.ASPECT_RATIO_16_9 * 140.dp).toInt(), 140.dp, false))
				addClassPresenterSelector(ExternalTrailer::class.java, ClassPresenterSelector().apply {
					addClassPresenter(YouTubeTrailer::class.java, YouTubeTrailerPresenter(requireContext(), 140.dp))
					addClassPresenter(ExternalTrailer::class.java, ExternalTrailerPresenter(requireContext(), 140.dp))
				})
			})
	) }
	private val streamInfoRow by lazy { createListRow("Media info", movie.mediaInfo.streams, InfoCardPresenter()) }

	override suspend fun onCreateAdapters(rowSelector: ClassPresenterSelector, rowAdapter: ArrayObjectAdapter) {
		super.onCreateAdapters(rowSelector, rowAdapter)

		// Retrieve additional info
		loadAdditionalInformation()


		// Add rows
		rowAdapter.apply {
			add(detailRow)
			addIfNotEmpty(chaptersRow)
			addIfNotEmpty(specialsRow)
			addIfNotEmpty(castRow)
			addIfNotEmpty(relatedItemsRow)
			addIfNotEmpty(trailersRow)
			addIfNotEmpty(streamInfoRow)
		}
	}

	private suspend fun loadAdditionalInformation() = withContext(Dispatchers.IO) {
		// Get additional information asynchronously
		awaitAll(
			async {
				val specials = TvApp.getApplication().apiClient.getSpecialFeatures(movie).orEmpty()
				(specialsRow.adapter as ArrayObjectAdapter).apply { specials.forEach(::add) }
			},
			async {
				//todo filter on server side?
				val relatedItems = TvApp.getApplication().apiClient.getSimilarItems(movie).orEmpty().filterIsInstance<Movie>()
				(relatedItemsRow.adapter as ArrayObjectAdapter).apply { relatedItems.forEach(::add) }
			},
			async {
				val trailers = TvApp.getApplication().apiClient.getLocalTrailers(movie).orEmpty()
				(trailersRow.adapter as ArrayObjectAdapter).apply {
					movie.externalTrailers.forEach(::add)
					trailers.forEach(::add)
				}
			}
		)
	}
}
