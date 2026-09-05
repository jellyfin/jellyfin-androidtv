package org.jellyfin.androidtv.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.BaseItemKind

class FavoritesViewModel(
	private val favoritesRepository: FavoritesRepository
) : ViewModel() {
	companion object {
		private val groups = mapOf(
			R.string.lbl_movies to setOf(BaseItemKind.MOVIE),
			R.string.lbl_series to setOf(BaseItemKind.SERIES),
			R.string.lbl_episodes to setOf(BaseItemKind.EPISODE),
			R.string.lbl_videos to setOf(BaseItemKind.VIDEO),
			R.string.lbl_playlists to setOf(BaseItemKind.PLAYLIST),
			R.string.lbl_artists to setOf(BaseItemKind.MUSIC_ARTIST),
			R.string.lbl_albums to setOf(BaseItemKind.MUSIC_ALBUM),
			R.string.lbl_songs to setOf(BaseItemKind.AUDIO),
			R.string.photo_albums to setOf(BaseItemKind.PHOTO_ALBUM),
			R.string.photos to setOf(BaseItemKind.PHOTO),
			R.string.lbl_collections to setOf(BaseItemKind.BOX_SET),
			R.string.lbl_people to setOf(BaseItemKind.PERSON),
		)
	}

	private val _favoritesResultsFlow = MutableStateFlow<Collection<FavoritesResultGroup>>(emptyList())
	val favoritesResultsFlow = _favoritesResultsFlow.asStateFlow()

	fun load() {
		viewModelScope.launch {
			_favoritesResultsFlow.value = groups.map { (stringRes, itemKinds) ->
				async {
					val result = favoritesRepository.getFavorites(itemKinds)
					val items = result.getOrNull().orEmpty()

					FavoritesResultGroup(stringRes, items)
				}
			}.awaitAll().filter { it.items.isNotEmpty() }
		}
	}
}
