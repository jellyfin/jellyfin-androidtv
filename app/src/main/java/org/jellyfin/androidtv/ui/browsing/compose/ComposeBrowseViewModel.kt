package org.jellyfin.androidtv.ui.browsing.compose

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.constant.Extras
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.composable.tv.MediaSection
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * ViewModel for Compose-based browsing that mirrors existing EnhancedBrowseFragment functionality
 * This preserves your existing data patterns while enabling Compose UI
 */
class ComposeBrowseViewModel : ViewModel(), KoinComponent {

	private val userViewsRepository by inject<UserViewsRepository>()
	private val itemRepository by inject<ItemRepository>()
	private val navigationRepository by inject<NavigationRepository>()
	private val backgroundService by inject<BackgroundService>()
	private val imageHelper by inject<ImageHelper>()
	private val itemLauncher by inject<ItemLauncher>()
	private val api by inject<ApiClient>()

	private val _uiState = MutableStateFlow(ComposeBrowseUiState())
	val uiState: StateFlow<ComposeBrowseUiState> = _uiState.asStateFlow()

	private var currentFolder: BaseItemDto? = null

	fun loadBrowseData(arguments: Bundle?) {
		viewModelScope.launch {
			try {
				_uiState.value = _uiState.value.copy(isLoading = true)

				// Parse folder from arguments (same as original EnhancedBrowseFragment)
				val folderJson = arguments?.getString(Extras.Folder)
				currentFolder = folderJson?.let {
					Json.decodeFromString(BaseItemDto.serializer(), it)
				}

				val sections = when (currentFolder?.collectionType) {
					CollectionType.MOVIES -> loadMovieSections()
					CollectionType.TVSHOWS -> loadTvShowSections()
					CollectionType.MUSIC -> loadMusicSections()
					CollectionType.LIVETV -> loadLiveTvSections()
					else -> loadDefaultSections()
				}

				_uiState.value = _uiState.value.copy(
					isLoading = false,
					sections = sections,
					title = currentFolder?.name ?: "Browse",
				)
			} catch (e: ApiClientException) {
				Timber.e(e, "Error loading browse data")
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					error = e.message,
				)
			}
		}
	}

	private suspend fun loadMovieSections(): List<MediaSection> {
		val sections = mutableListOf<MediaSection>()

		try {
			// Continue Watching Movies
			val resumeItems = loadResumeItems(BaseItemKind.MOVIE)
			if (resumeItems.isNotEmpty()) {
				sections.add(MediaSection("Continue Watching", resumeItems))
			}

			// Latest Movies
			val latestItems = loadLatestItems(BaseItemKind.MOVIE)
			if (latestItems.isNotEmpty()) {
				sections.add(MediaSection("Latest Movies", latestItems))
			}

			// Favorite Movies
			val favoriteItems = loadFavoriteItems(BaseItemKind.MOVIE)
			if (favoriteItems.isNotEmpty()) {
				sections.add(MediaSection("Favorites", favoriteItems))
			}
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading movie sections")
		}

		return sections
	}

	private suspend fun loadTvShowSections(): List<MediaSection> {
		val sections = mutableListOf<MediaSection>()

		try {
			// Next Up Episodes
			val nextUpItems = loadNextUpItems()
			if (nextUpItems.isNotEmpty()) {
				sections.add(MediaSection("Next Up", nextUpItems))
			}

			// Continue Watching Episodes
			val resumeItems = loadResumeItems(BaseItemKind.EPISODE)
			if (resumeItems.isNotEmpty()) {
				sections.add(MediaSection("Continue Watching", resumeItems))
			}

			// Latest Episodes
			val latestItems = loadLatestItems(BaseItemKind.EPISODE)
			if (latestItems.isNotEmpty()) {
				sections.add(MediaSection("Latest Episodes", latestItems))
			}

			// Favorite Series
			val favoriteItems = loadFavoriteItems(BaseItemKind.SERIES)
			if (favoriteItems.isNotEmpty()) {
				sections.add(MediaSection("Favorite Series", favoriteItems))
			}
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading TV show sections")
		}

		return sections
	}

	private suspend fun loadMusicSections(): List<MediaSection> {
		val sections = mutableListOf<MediaSection>()

		try {
			// Recently Played Albums
			val recentItems = loadRecentlyPlayedItems()
			if (recentItems.isNotEmpty()) {
				sections.add(MediaSection("Recently Played", recentItems))
			}

			// Latest Albums
			val latestItems = loadLatestItems(BaseItemKind.MUSIC_ALBUM)
			if (latestItems.isNotEmpty()) {
				sections.add(MediaSection("Latest Albums", latestItems))
			}

			// Favorite Albums
			val favoriteItems = loadFavoriteItems(BaseItemKind.MUSIC_ALBUM)
			if (favoriteItems.isNotEmpty()) {
				sections.add(MediaSection("Favorite Albums", favoriteItems))
			}
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading music sections")
		}

		return sections
	}

	private suspend fun loadLiveTvSections(): List<MediaSection> {
		val sections = mutableListOf<MediaSection>()

		try {
			// On Now
			// Latest Recordings
			// Upcoming
			// Note: These would use your existing LiveTV API calls
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading Live TV sections")
		}

		return sections
	}

	private suspend fun loadDefaultSections(): List<MediaSection> {
		// For collection folders or other types
		return emptyList()
	}

	// Helper methods that would use your existing repository calls
	private suspend fun loadResumeItems(itemType: BaseItemKind): List<BaseItemDto> {
		return try {
			// Use existing ItemRepository methods
			// This is a placeholder - you'd adapt your existing resume items logic
			emptyList()
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading resume items")
			emptyList()
		}
	}

	private suspend fun loadLatestItems(itemType: BaseItemKind): List<BaseItemDto> {
		return try {
			// Use existing ItemRepository methods for latest items
			emptyList()
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading latest items")
			emptyList()
		}
	}

	private suspend fun loadFavoriteItems(itemType: BaseItemKind): List<BaseItemDto> {
		return try {
			// Use existing ItemRepository methods for favorites
			emptyList()
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading favorite items")
			emptyList()
		}
	}

	private suspend fun loadNextUpItems(): List<BaseItemDto> {
		return try {
			// Use existing next up logic
			emptyList()
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading next up items")
			emptyList()
		}
	}

	private suspend fun loadRecentlyPlayedItems(): List<BaseItemDto> {
		return try {
			// Use existing recently played logic
			emptyList()
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading recently played items")
			emptyList()
		}
	}

	fun onItemClick(item: BaseItemDto) {
		// Use your existing ItemLauncher logic
		when (item.type) {
			BaseItemKind.MOVIE,
			BaseItemKind.EPISODE,
			-> {
				// Launch playback or details
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
			BaseItemKind.SERIES -> {
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
			BaseItemKind.COLLECTION_FOLDER -> {
				navigationRepository.navigate(Destinations.libraryBrowser(item))
			}
			else -> {
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
		}
	}

	fun onItemFocus(item: BaseItemDto) {
		_uiState.value = _uiState.value.copy(focusedItem = item)

		// Update background using existing service
		backgroundService.setBackground(item)
	}

	fun getItemImageUrl(item: BaseItemDto): String? = imageHelper.getPrimaryImageUrl(item, null, 300)
}

/**
 * UI State for the Compose browse screen
 */
data class ComposeBrowseUiState(
	val isLoading: Boolean = true,
	val sections: List<MediaSection> = emptyList(),
	val error: String? = null,
	val focusedItem: BaseItemDto? = null,
	val title: String = "Browse",
)
