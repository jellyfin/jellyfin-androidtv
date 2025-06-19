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
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.composable.tv.ImmersiveListLayout
import org.jellyfin.androidtv.ui.composable.tv.ImmersiveListSection
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.UUID

/**
 * ViewModel for the Compose Music library screen with immersive list components
 */
class ComposeMusicViewModel : ViewModel(), KoinComponent {

	private val apiClient by inject<ApiClient>()
	private val itemRepository by inject<ItemRepository>()
	private val navigationRepository by inject<NavigationRepository>()
	private val backgroundService by inject<BackgroundService>()
	private val imageHelper by inject<ImageHelper>()

	private val _uiState = MutableStateFlow(MusicUiState())
	val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

	private var currentFolder: BaseItemDto? = null

	fun loadMusicData(arguments: Bundle?) {
		viewModelScope.launch {
			try {
				_uiState.value = _uiState.value.copy(isLoading = true)

				// Parse folder from arguments (same as BrowseViewFragment)
				val folderJson = arguments?.getString(Extras.Folder)
				currentFolder = folderJson?.let {
					Json.decodeFromString(BaseItemDto.serializer(), it)
				}

				Timber.d("ComposeMusicViewModel: Loading data for folder: ${currentFolder?.name}")

				val sections = loadMusicSections()

				_uiState.value = _uiState.value.copy(
					isLoading = false,
					sections = sections,
					title = currentFolder?.name ?: "Music",
					folder = currentFolder,
				)

				Timber.d("ComposeMusicViewModel: Loaded ${sections.size} sections")
			} catch (e: ApiClientException) {
				Timber.e(e, "Error loading music data")
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					error = e.message,
				)
			}
		}
	}

	private suspend fun loadMusicSections(): List<ImmersiveListSection> {
		val sections = mutableListOf<ImmersiveListSection>()
		val folderId = currentFolder?.id

		if (folderId == null) {
			Timber.w("No folder ID provided")
			return sections
		}

		try {
			// Latest Audio (matching BrowseViewFragment music case)
			val latestAudio = loadLatestAudio(folderId)
			if (latestAudio.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Latest",
						items = latestAudio,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
				Timber.d("Added Latest Audio section with ${latestAudio.size} items")
			}

			// Last Played
			val lastPlayed = loadLastPlayedAudio(folderId)
			if (lastPlayed.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Last Played",
						items = lastPlayed,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
				Timber.d("Added Last Played section with ${lastPlayed.size} items")
			}

			// Favorites (Music Albums)
			val favoriteAlbums = loadFavoriteAlbums(folderId)
			if (favoriteAlbums.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Favorites",
						items = favoriteAlbums,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
				Timber.d("Added Favorites section with ${favoriteAlbums.size} items")
			}

			// Audio Playlists
			val playlists = loadAudioPlaylists()
			if (playlists.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Playlists",
						items = playlists,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
				Timber.d("Added Playlists section with ${playlists.size} items")
			}
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading music sections")
		}

		return sections
	}

	private suspend fun loadLatestAudio(folderId: UUID): List<BaseItemDto> {
		return try {
			val response = apiClient.userLibraryApi.getLatestMedia(
				parentId = folderId,
				fields = ItemRepository.itemFields,
				includeItemTypes = setOf(BaseItemKind.AUDIO),
				limit = 50,
				imageTypeLimit = 1,
				groupItems = true,
			)
			response.content
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading latest audio")
			emptyList()
		}
	}

	private suspend fun loadLastPlayedAudio(folderId: UUID): List<BaseItemDto> {
		return try {
			val response = apiClient.itemsApi.getItems(
				parentId = folderId,
				fields = ItemRepository.itemFields,
				includeItemTypes = setOf(BaseItemKind.AUDIO),
				recursive = true,
				imageTypeLimit = 1,
				filters = setOf(ItemFilter.IS_PLAYED),
				sortBy = setOf(ItemSortBy.DATE_PLAYED),
				sortOrder = setOf(SortOrder.DESCENDING),
				limit = 50,
			)
			response.content.items
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading last played audio")
			emptyList()
		}
	}

	private suspend fun loadFavoriteAlbums(folderId: UUID): List<BaseItemDto> {
		return try {
			val response = apiClient.itemsApi.getItems(
				parentId = folderId,
				fields = ItemRepository.itemFields,
				includeItemTypes = setOf(BaseItemKind.MUSIC_ALBUM),
				recursive = true,
				imageTypeLimit = 1,
				filters = setOf(ItemFilter.IS_FAVORITE),
				sortBy = setOf(ItemSortBy.SORT_NAME),
				limit = 60,
			)
			response.content.items
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading favorite albums")
			emptyList()
		}
	}

	private suspend fun loadAudioPlaylists(): List<BaseItemDto> {
		return try {
			val response = apiClient.itemsApi.getItems(
				fields = ItemRepository.itemFields,
				includeItemTypes = setOf(BaseItemKind.PLAYLIST),
				imageTypeLimit = 1,
				recursive = true,
				sortBy = setOf(ItemSortBy.DATE_CREATED),
				sortOrder = setOf(SortOrder.DESCENDING),
				limit = 60,
			)
			response.content.items
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading audio playlists")
			emptyList()
		}
	}

	fun onItemClick(item: BaseItemDto) {
		Timber.d("Item clicked: ${item.name}")
		when (item.type) {
			BaseItemKind.AUDIO -> {
				// Navigate to audio details or start playback
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
			BaseItemKind.MUSIC_ALBUM -> {
				// Navigate to album details
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
			BaseItemKind.PLAYLIST -> {
				// Navigate to playlist details
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
			else -> {
				// Handle other item types if needed
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
		}
	}

	fun onItemFocus(item: BaseItemDto) {
		Timber.d("Item focused: ${item.name}")
		_uiState.value = _uiState.value.copy(focusedItem = item)

		// Update background using existing service
		backgroundService.setBackground(item)
	}

	fun getItemImageUrl(item: BaseItemDto): String? {
		// For music items, prefer primary images (album art)
		return imageHelper.getPrimaryImageUrl(item, null, 400)
	}
	
	fun getItemBackdropUrl(item: BaseItemDto): String? = item.itemBackdropImages.firstOrNull()?.getUrl(
		api = apiClient,
		maxWidth = 1920,
		maxHeight = 1080,
	)

	fun getItemLogoUrl(item: BaseItemDto): String? {
		return try {
			// Use ImageHelper to get logo URL directly
			imageHelper.getLogoImageUrl(item, 400)
		} catch (e: ApiClientException) {
			Timber.e(e, "Failed to get logo for item: ${item.name}")
			null
		}
	}
}

/**
 * UI State for the Music screen
 */
data class MusicUiState(
	val isLoading: Boolean = true,
	val sections: List<ImmersiveListSection> = emptyList(),
	val error: String? = null,
	val focusedItem: BaseItemDto? = null,
	val title: String = "Music",
	val folder: BaseItemDto? = null,
)
