package org.jellyfin.androidtv.details

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.lifecycle.MutableLiveData
import org.jellyfin.androidtv.details.actions.*
import org.jellyfin.androidtv.model.itemtypes.Episode
import org.jellyfin.androidtv.presentation.InfoCardPresenter
import org.jellyfin.androidtv.util.addIfNotEmpty

class EpisodeDetailsFragment(private val episode: Episode) : BaseDetailsFragment<Episode>(episode) {
	// Action definitions
	private val actions by lazy {
		val item = MutableLiveData(episode)

		listOf(
			ResumeAction(context!!, item),
			PlayFromBeginningAction(context!!, item),
			ToggleWatchedAction(context!!, item),
			ToggleFavoriteAction(context!!, item),

			// "More" button
			SecondariesPopupAction(context!!, listOf(
				// TODO: Go to Series Button
				// TODO: Go to Season Button
				DeleteAction(context!!, item) { activity?.finish() }
			))
		)
	}

	// Row definitions
	private val detailRow by lazy { DetailsOverviewRow(episode, actions, episode.images.logo, episode.images.backdrops) }
	// TODO: More from this season row
	private val chaptersRow by lazy { createListRow("Chapters", episode.chapters, ChapterInfoPresenter(context!!)) }
	private val streamInfoRow by lazy { createListRow("Media info", episode.mediaInfo.streams, InfoCardPresenter()) }

	override suspend fun onCreateAdapters(rowSelector: ClassPresenterSelector, rowAdapter: ArrayObjectAdapter) {
		super.onCreateAdapters(rowSelector, rowAdapter)

		// Add rows
		rowAdapter.apply {
			add(detailRow)
			addIfNotEmpty(chaptersRow)
			addIfNotEmpty(streamInfoRow)
		}
	}
}
