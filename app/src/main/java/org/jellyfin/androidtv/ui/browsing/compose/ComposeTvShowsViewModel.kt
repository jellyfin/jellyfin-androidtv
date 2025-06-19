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
import org.jellyfin.androidtv.util.apiclient.seriesPrimaryImage
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
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
 * ViewModel for the Compose TV Shows library screen with immersive list components
 * Supports navigation: TV Shows → Seasons → Episodes
 */
class ComposeTvShowsViewModel : ViewModel(), KoinComponent {

	private val apiClient by inject<ApiClient>()
	private val itemRepository by inject<ItemRepository>()
	private val navigationRepository by inject<NavigationRepository>()
	private val backgroundService by inject<BackgroundService>()
	private val imageHelper by inject<ImageHelper>()

	private val _uiState = MutableStateFlow(TvShowsUiState())
	val uiState: StateFlow<TvShowsUiState> = _uiState.asStateFlow()

	private var currentFolder: BaseItemDto? = null

	fun loadTvShowsData(arguments: Bundle?) {
		viewModelScope.launch {
			try {
				_uiState.value = _uiState.value.copy(isLoading = true)

				// Parse folder from arguments (same as BrowseGridFragment)
				val folderJson = arguments?.getString(Extras.Folder)
				currentFolder = folderJson?.let {
					Json.decodeFromString(BaseItemDto.serializer(), it)
				}

				Timber.d("ComposeTvShowsViewModel: Loading data for folder: ${currentFolder?.name}")

				val sections = loadTvShowSections()

				_uiState.value = _uiState.value.copy(
					isLoading = false,
					sections = sections,
					title = currentFolder?.name ?: "TV Shows",
					folder = currentFolder,
				)

				Timber.d("ComposeTvShowsViewModel: Loaded ${sections.size} sections")
			} catch (e: ApiClientException) {
				Timber.e(e, "Error loading TV shows data")
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					error = e.message,
				)
			}
		}
	}

	private suspend fun loadTvShowSections(): List<ImmersiveListSection> {
		val sections = mutableListOf<ImmersiveListSection>()
		val folderId = currentFolder?.id

		if (folderId == null) {
			Timber.w("No folder ID provided")
			return sections
		}

		try {
			// All TV Series (Grid View) - Main grid showing all series
			val allSeries = loadAllSeries(folderId)
			if (allSeries.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "All TV Series",
						items = allSeries,
						layout = ImmersiveListLayout.VERTICAL_GRID,
					),
				)
				Timber.d("Added All TV Series section with ${allSeries.size} items")
			}

			// Favorite Series - Use vertical grid like All TV Series since these are series
			val favoriteSeries = loadFavoriteSeries(folderId)
			if (favoriteSeries.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Favorite Series",
						items = favoriteSeries,
						layout = ImmersiveListLayout.VERTICAL_GRID,
					),
				)
				Timber.d("Added Favorite Series section with ${favoriteSeries.size} items")
			}
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading TV show sections")
		}

		return sections
	}

	private suspend fun loadAllSeries(folderId: UUID): List<BaseItemDto> {
		return try {
			val response = apiClient.itemsApi.getItems(
				parentId = folderId,
				includeItemTypes = setOf(BaseItemKind.SERIES),
				recursive = true,
				fields = ItemRepository.itemFields,
				sortBy = setOf(ItemSortBy.SORT_NAME),
				sortOrder = setOf(SortOrder.ASCENDING),
				limit = 200,
			)
			response.content.items
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading all series")
			emptyList()
		}
	}

	private suspend fun loadFavoriteSeries(folderId: UUID): List<BaseItemDto> {
		return try {
			val response = apiClient.itemsApi.getItems(
				parentId = folderId,
				includeItemTypes = setOf(BaseItemKind.SERIES),
				recursive = true,
				fields = ItemRepository.itemFields,
				sortBy = setOf(ItemSortBy.SORT_NAME),
				sortOrder = setOf(SortOrder.ASCENDING),
				filters = setOf(ItemFilter.IS_FAVORITE),
				limit = 50,
			)
			response.content.items
		} catch (e: ApiClientException) {
			Timber.e(e, "Error loading favorite series")
			emptyList()
		}
	}

	fun onItemClick(item: BaseItemDto) {
		Timber.d("Item clicked: ${item.name} (Type: ${item.type})")
		when (item.type) {
			BaseItemKind.SERIES -> {
				// Navigate to Compose series details (seasons view)
				navigationRepository.navigate(Destinations.composeSeriesDetail(item.id))
			}
			BaseItemKind.SEASON -> {
				// Navigate to Compose season details (episodes view)
				navigationRepository.navigate(Destinations.composeSeasonDetail(item.id))
			}
			BaseItemKind.EPISODE -> {
				// For episodes, navigate to item details for playback
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
		return when (item.type) {
			BaseItemKind.EPISODE -> {
				// For episodes, prefer series primary image for consistency
				try {
					// Use the built-in seriesPrimaryImage from the item if available
					val seriesImageUrl = item.seriesPrimaryImage?.getUrl(
						api = apiClient,
						maxWidth = 400,
						maxHeight = 600,
					)
					// Fallback: use episode primary image
					seriesImageUrl ?: imageHelper.getPrimaryImageUrl(item, null, 400)
				} catch (e: ApiClientException) {
					Timber.e(e, "Failed to get image for episode: ${item.name}")
					imageHelper.getPrimaryImageUrl(item, null, 400)
				}
			}
			BaseItemKind.SERIES -> {
				// For series in horizontal cards, prefer backdrop images (landscape)
				// This provides the correct aspect ratio for horizontal card layouts
				val backdropUrl = getItemBackdropUrl(item)
				backdropUrl ?: imageHelper.getPrimaryImageUrl(item, null, 400)
			}
			else -> {
				// Fallback for other types
				imageHelper.getPrimaryImageUrl(item, null, 400)
			}
		}
	}
	
	fun getItemBackdropUrl(item: BaseItemDto): String? {
		return when (item.type) {
			BaseItemKind.EPISODE -> {
				// For episodes, prefer episode's own backdrop if available
				try {
					item.itemBackdropImages.firstOrNull()?.getUrl(
						api = apiClient,
						maxWidth = 1920,
						maxHeight = 1080,
					)
				} catch (e: ApiClientException) {
					Timber.e(e, "Failed to get backdrop for episode: ${item.name}")
					null
				}
			}
			else -> {
				// For series and other types, use their own backdrop
				item.itemBackdropImages.firstOrNull()?.getUrl(
					api = apiClient,
					maxWidth = 1920,
					maxHeight = 1080,
				)
			}
		}
	}

	fun getItemLogoUrl(item: BaseItemDto): String? {
		return try {
			// Use ImageHelper to get logo URL directly
			imageHelper.getLogoImageUrl(item, 400)
		} catch (e: IllegalArgumentException) {
			Timber.e(e, "Failed to get logo for item: ${item.name}")
			null
		}
	}
}

/**
 * UI State for the TV Shows screen
 */
data class TvShowsUiState(
	val isLoading: Boolean = true,
	val sections: List<ImmersiveListSection> = emptyList(),
	val error: String? = null,
	val focusedItem: BaseItemDto? = null,
	val title: String = "TV Shows",
	val folder: BaseItemDto? = null,
)
