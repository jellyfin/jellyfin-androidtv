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
import org.jellyfin.androidtv.details.actions.ToggleFavoriteAction
import org.jellyfin.androidtv.details.presenters.ItemPresenter
import org.jellyfin.androidtv.model.itemtypes.Artist
import org.jellyfin.androidtv.util.addIfNotEmpty
import org.jellyfin.androidtv.util.apiclient.getSimilarItems
import org.jellyfin.androidtv.util.dp

class ArtistDetailsFragment(private val artist: Artist) : BaseDetailsFragment<Artist>(artist) {
	// Action definitions
	private val actions by lazy {
		val item = MutableLiveData(artist)

		listOf(
//			PlayAction(requireContext(), item),
//			InstantMixAction(requireContext()),
//			ShuffleAction(requireContext()),
			ToggleFavoriteAction(requireContext(), item)
//			SecondariesPopupAction(requireContext(), listOf(
//				AddToCollectionAction(),
//				AddToPlaylistAction()
//			))
		)
	}

	// Row definitions
	private val detailRow by lazy { DetailsOverviewRow(artist, actions, artist.images.primary, artist.images.backdrops) }
	private val relatedItemsRow by lazy { createListRow(getString(R.string.lbl_similar_items_library), emptyList(), ItemPresenter(requireContext(), 150.dp, 150.dp, false)) }

	override suspend fun onCreateAdapters(rowSelector: ClassPresenterSelector, rowAdapter: ArrayObjectAdapter) {
		super.onCreateAdapters(rowSelector, rowAdapter)

		// Retrieve additional info
		loadAdditionalInformation()

		// Add rows
		rowAdapter.apply {
			add(detailRow)
			// discography (albums/songs)
			addIfNotEmpty(relatedItemsRow)
		}
	}

	private suspend fun loadAdditionalInformation() = withContext(Dispatchers.IO) {
		// Get additional information asynchronously
		awaitAll(
			async {
				val relatedItems = TvApp.getApplication().apiClient.getSimilarItems(artist).orEmpty()
				(relatedItemsRow.adapter as ArrayObjectAdapter).apply { relatedItems.forEach(::add) }
			}
		)
	}
}
