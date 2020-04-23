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
import org.jellyfin.androidtv.details.actions.InstantMixAction
import org.jellyfin.androidtv.details.actions.PlayFromBeginningAction
import org.jellyfin.androidtv.details.actions.ShuffleAction
import org.jellyfin.androidtv.details.actions.ToggleFavoriteAction
import org.jellyfin.androidtv.details.presenters.ItemPresenter
import org.jellyfin.androidtv.details.presenters.SongPresenter
import org.jellyfin.androidtv.details.rows.DetailsOverviewRow
import org.jellyfin.androidtv.model.itemtypes.Album
import org.jellyfin.androidtv.util.addIfNotEmpty
import org.jellyfin.androidtv.util.apiclient.getAlbumsForArtists
import org.jellyfin.androidtv.util.apiclient.getSimilarItems
import org.jellyfin.androidtv.util.apiclient.getSongsForAlbum
import org.jellyfin.androidtv.util.dp

class AlbumDetailsFragment(private val album: Album) : BaseDetailsFragment<Album>(album) {
	// Action definitions
	private val actions by lazy {
		val item = MutableLiveData(album)

		listOf(
			PlayFromBeginningAction(requireContext(), item),
			InstantMixAction(requireContext(), item),
			ShuffleAction(requireContext(), item),
			ToggleFavoriteAction(requireContext(), item)
		)
	}

	// Row definitions
	private val detailRow by lazy { DetailsOverviewRow(album, actions, album.images.primary, album.images.backdrops) }
	private val songsRow by lazy { createVerticalListRow(getString(R.string.lbl_songs), SongPresenter()) }
	private val relatedDiscographyRow by lazy { createListRow(getString(R.string.lbl_more_from_x, album.artist.joinToString(", ") { it.name }), emptyList(), ItemPresenter(requireContext(), 150.dp, 150.dp, false)) }
	private val relatedItemsRow by lazy { createListRow(getString(R.string.lbl_similar_items_library), emptyList(), ItemPresenter(requireContext(), 150.dp, 150.dp, false)) }

	override suspend fun onCreateAdapters(rowSelector: ClassPresenterSelector, rowAdapter: ArrayObjectAdapter) {
		super.onCreateAdapters(rowSelector, rowAdapter)

		// Retrieve additional info
		loadAdditionalInformation()

		// Add rows
		rowAdapter.apply {
			add(detailRow)
			addIfNotEmpty(songsRow)
			addIfNotEmpty(relatedDiscographyRow)
			addIfNotEmpty(relatedItemsRow)
		}
	}

	private suspend fun loadAdditionalInformation() = withContext(Dispatchers.IO) {
		// Get additional information asynchronously
		awaitAll(
			async {
				val songs = TvApp.getApplication().apiClient.getSongsForAlbum(album.id).orEmpty()
				(songsRow.adapter as ArrayObjectAdapter).apply { songs.forEach(::add) }
			},
			async {
				val relatedDiscography = TvApp.getApplication().apiClient.getAlbumsForArtists(album.artist.map { it.id }.toTypedArray()).orEmpty()
				(relatedDiscographyRow.adapter as ArrayObjectAdapter).apply { relatedDiscography.forEach(::add) }
			},
			async {
				val relatedItems = TvApp.getApplication().apiClient.getSimilarItems(album).orEmpty()
				(relatedItemsRow.adapter as ArrayObjectAdapter).apply { relatedItems.forEach(::add) }
			}
		)
	}
}

