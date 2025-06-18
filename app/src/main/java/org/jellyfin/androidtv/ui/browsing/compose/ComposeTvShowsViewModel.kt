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
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
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
			} catch (e: Exception) {
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
			// Continue Watching Episodes - Episodes that user has started but not finished
			val continueWatchingEpisodes = loadContinueWatchingEpisodes(folderId)
			if (continueWatchingEpisodes.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Continue Watching",
						items = continueWatchingEpisodes,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
				Timber.d("Added Continue Watching section with ${continueWatchingEpisodes.size} items")
			}

			// Next Up Episodes - Next episodes to watch for series in progress
			val nextUpEpisodes = loadNextUpEpisodes(folderId)
			if (nextUpEpisodes.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Next Up",
						items = nextUpEpisodes,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
				Timber.d("Added Next Up section with ${nextUpEpisodes.size} items")
			}

			// Latest Episodes - Recently added episodes
			val latestEpisodes = loadLatestEpisodes(folderId)
			if (latestEpisodes.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Latest Episodes",
						items = latestEpisodes,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
				Timber.d("Added Latest Episodes section with ${latestEpisodes.size} items")
			}

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

			// Favorite Series
			val favoriteSeries = loadFavoriteSeries(folderId)
			if (favoriteSeries.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Favorite Series",
						items = favoriteSeries,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
				Timber.d("Added Favorite Series section with ${favoriteSeries.size} items")
			}
		} catch (e: Exception) {
			Timber.e(e, "Error loading TV show sections")
		}

		return sections
	}

	private suspend fun loadContinueWatchingEpisodes(folderId: UUID): List<BaseItemDto> {
		return try {
			val response = apiClient.itemsApi.getItems(
				parentId = folderId,
				includeItemTypes = setOf(BaseItemKind.EPISODE),
				recursive = true,
				fields = ItemRepository.itemFields,
				sortBy = setOf(ItemSortBy.DATE_PLAYED),
				sortOrder = setOf(SortOrder.DESCENDING),
				filters = setOf(ItemFilter.IS_RESUMABLE),
				limit = 50,
			)
			response.content.items
		} catch (e: Exception) {
			Timber.e(e, "Error loading continue watching episodes")
			emptyList()
		}
	}

	private suspend fun loadNextUpEpisodes(folderId: UUID): List<BaseItemDto> {
		return try {
			val response = apiClient.tvShowsApi.getNextUp(
				parentId = folderId,
				fields = ItemRepository.itemFields,
				limit = 50,
			)
			response.content.items
		} catch (e: Exception) {
			Timber.e(e, "Error loading next up episodes")
			emptyList()
		}
	}

	private suspend fun loadLatestEpisodes(folderId: UUID): List<BaseItemDto> {
		return try {
			val response = apiClient.itemsApi.getItems(
				parentId = folderId,
				includeItemTypes = setOf(BaseItemKind.EPISODE),
				recursive = true,
				fields = ItemRepository.itemFields,
				sortBy = setOf(ItemSortBy.DATE_CREATED),
				sortOrder = setOf(SortOrder.DESCENDING),
				filters = setOf(ItemFilter.IS_UNPLAYED),
				limit = 50,
			)
			response.content.items
		} catch (e: Exception) {
			Timber.e(e, "Error loading latest episodes")
			emptyList()
		}
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
		} catch (e: Exception) {
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
		} catch (e: Exception) {
			Timber.e(e, "Error loading favorite series")
			emptyList()
		}
	}

	fun onItemClick(item: BaseItemDto) {
		Timber.d("Item clicked: ${item.name} (Type: ${item.type})")
		when (item.type) {
			BaseItemKind.SERIES -> {
				// Navigate to series details (seasons view)
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
			BaseItemKind.SEASON -> {
				// Navigate to season details (episodes view)
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
			BaseItemKind.EPISODE -> {
				// Navigate to episode details or start playback
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
		// For horizontal cards (episodes), prefer backdrop images first, then fall back to primary
		return if (item.type == BaseItemKind.EPISODE) {
			getItemBackdropUrl(item) ?: imageHelper.getPrimaryImageUrl(item, null, 400)
		} else {
			// For series, use primary images (poster style)
			imageHelper.getPrimaryImageUrl(item, null, 400)
		}
	}
	
	fun getItemBackdropUrl(item: BaseItemDto): String? {
		return item.itemBackdropImages.firstOrNull()?.getUrl(
			api = apiClient,
			maxWidth = 1920,
			maxHeight = 1080,
		)
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
