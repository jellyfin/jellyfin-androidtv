package org.jellyfin.androidtv.details.fragments

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.details.DetailsOverviewRow
import org.jellyfin.androidtv.details.actions.*
import org.jellyfin.androidtv.details.presenters.ChapterInfoPresenter
import org.jellyfin.androidtv.details.presenters.ItemPresenter
import org.jellyfin.androidtv.details.presenters.PersonPresenter
import org.jellyfin.androidtv.model.itemtypes.Episode
import org.jellyfin.androidtv.presentation.InfoCardPresenter
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.addIfNotEmpty
import org.jellyfin.androidtv.util.apiclient.getEpisodesOfSeason
import org.jellyfin.androidtv.util.dp

class EpisodeDetailsFragment(private val episode: Episode) : BaseDetailsFragment<Episode>(episode) {
	// Action definitions
	private val actions by lazy {
		val item = MutableLiveData(episode)

		listOf(
			ResumeAction(requireContext(), item),
			PlayFromBeginningAction(requireContext(), item),
			ToggleWatchedAction(requireContext(), item),
			ToggleFavoriteAction(requireContext(), item),

			// "More" button
			SecondariesPopupAction(requireContext(), listOfNotNull(
				episode.seasonId?.let {
					GoToItemAction(requireContext(), requireContext().getString(R.string.lbl_goto_season), it)
				},
				episode.seriesId?.let {
					GoToItemAction(requireContext(), requireContext().getString(R.string.lbl_goto_series), it)
				},
				DeleteAction(requireContext(), item) { activity?.finish() }
			))
		)
	}

	// Row definitions
	private val detailRow by lazy {
		val primary = episode.images.parentPrimary ?: episode.seasonPrimaryImage
		?: episode.seriesPrimaryImage ?: episode.images.primary
		val backdrops = episode.images.parentBackdrops ?: episode.images.backdrops
		DetailsOverviewRow(episode, actions, primary, backdrops)
	}
	private val moreFromThisSeason by lazy {
		createListRow(
			requireContext().getString(R.string.lbl_more_from_this_season),
			emptyList(),
			ItemPresenter(requireContext(), (ImageUtils.ASPECT_RATIO_16_9 * 140.dp).toInt(), 140.dp, true)
		)
	}
	private val chaptersRow by lazy {
		createListRow(
			requireContext().getString(R.string.chapters),
			episode.chapters,
			ChapterInfoPresenter(requireContext())
		)
	}

	private val cast by lazy {
		createListRow(
			requireContext().getString(R.string.lbl_cast_crew),
			episode.cast,
			PersonPresenter(requireContext())
		)
	}

	private val streamInfoRow by lazy {
		createListRow(
			requireContext().getString(R.string.lbl_media_info),
			episode.mediaInfo.streams,
			InfoCardPresenter()
		)
	}

	override suspend fun onCreateAdapters(rowSelector: ClassPresenterSelector, rowAdapter: ArrayObjectAdapter) {
		super.onCreateAdapters(rowSelector, rowAdapter)

		loadAdditionalInformation()

		// Add rows
		rowAdapter.apply {
			add(detailRow)
			addIfNotEmpty(moreFromThisSeason)
			addIfNotEmpty(chaptersRow)
			addIfNotEmpty(cast)
			addIfNotEmpty(streamInfoRow)
		}
	}

	private suspend fun loadAdditionalInformation() = withContext(Dispatchers.IO) {
		// Get additional information asynchronously
		awaitAll(
			async {
				TvApp.getApplication().apiClient.getEpisodesOfSeason(episode)?.let { episodes ->
					val adapter = (moreFromThisSeason.adapter as ArrayObjectAdapter)
					adapter.apply { episodes.forEach(::add) }
				}
			}
		)
	}
}
