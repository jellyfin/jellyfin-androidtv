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
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.UUID

/**
 * ViewModel for the Compose Movies library screen with immersive list components
 */
class ComposeMoviesViewModel : ViewModel(), KoinComponent {

	private val apiClient by inject<ApiClient>()
	private val itemRepository by inject<ItemRepository>()
	private val navigationRepository by inject<NavigationRepository>()
	private val backgroundService by inject<BackgroundService>()
	private val imageHelper by inject<ImageHelper>()

	private val _uiState = MutableStateFlow(MoviesUiState())
	val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

	private var currentFolder: BaseItemDto? = null

	fun loadMoviesData(arguments: Bundle?) {
		viewModelScope.launch {
			try {
				_uiState.value = _uiState.value.copy(isLoading = true)

				// Parse folder from arguments (same as BrowseGridFragment)
				val folderJson = arguments?.getString(Extras.Folder)
				currentFolder = folderJson?.let {
					Json.decodeFromString(BaseItemDto.serializer(), it)
				}

				Timber.d("ComposeMoviesViewModel: Loading data for folder: ${currentFolder?.name}")

				val sections = loadMovieSections()

				_uiState.value = _uiState.value.copy(
					isLoading = false,
					sections = sections,
					title = currentFolder?.name ?: "Movies",
					folder = currentFolder,
				)

				Timber.d("ComposeMoviesViewModel: Loaded ${sections.size} sections")
			} catch (e: Exception) {
				Timber.e(e, "Error loading movies data")
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					error = e.message,
				)
			}
		}
	}

	private suspend fun loadMovieSections(): List<ImmersiveListSection> {
		val sections = mutableListOf<ImmersiveListSection>()
		val folderId = currentFolder?.id

		if (folderId == null) {
			Timber.w("No folder ID provided")
			return sections
		}

		try {
			// All Movies (Grid View) - Single section showing all movies
			val allMovies = loadAllMovies(folderId)
			if (allMovies.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Movies",
						items = allMovies,
						layout = ImmersiveListLayout.VERTICAL_GRID,
					),
				)
				Timber.d("Added Movies section with ${allMovies.size} items")
			}
		} catch (e: Exception) {
			Timber.e(e, "Error loading movie sections")
		}

		return sections
	}

	private suspend fun loadAllMovies(folderId: UUID): List<BaseItemDto> {
		return try {
			val response = apiClient.itemsApi.getItems(
				parentId = folderId,
				includeItemTypes = setOf(BaseItemKind.MOVIE),
				recursive = true,
				fields = ItemRepository.itemFields,
				sortBy = setOf(ItemSortBy.SORT_NAME),
				sortOrder = setOf(SortOrder.ASCENDING),
				limit = 200, // Increased limit since this is now the primary view
			)
			response.content.items
		} catch (e: Exception) {
			Timber.e(e, "Error loading all movies")
			emptyList()
		}
	}

	fun onItemClick(item: BaseItemDto) {
		Timber.d("Item clicked: ${item.name}")
		when (item.type) {
			BaseItemKind.MOVIE -> {
				// Navigate to movie details or start playback
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
		return imageHelper.getPrimaryImageUrl(item, null, 400)
	}
	fun getItemBackdropUrl(item: BaseItemDto): String? {
		return item.backdropImageTags?.firstOrNull()?.let { _ ->
			// Use the primary image if no backdrop, as a fallback
			imageHelper.getPrimaryImageUrl(item, null, 1920)
		} ?: run {
			// Fallback to primary image if no backdrop
			imageHelper.getPrimaryImageUrl(item, null, 1920)
		}
	}
}

/**
 * UI State for the Movies screen
 */
data class MoviesUiState(
	val isLoading: Boolean = true,
	val sections: List<ImmersiveListSection> = emptyList(),
	val error: String? = null,
	val focusedItem: BaseItemDto? = null,
	val title: String = "Movies",
	val folder: BaseItemDto? = null,
)
