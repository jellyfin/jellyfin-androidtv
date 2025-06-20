package org.jellyfin.androidtv.ui.browsing.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.repository.ItemRepository
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
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.UUID

/**
 * ViewModel for the TV Show seasons screen
 * Displays all seasons for a specific TV show
 */
class ComposeSeasonViewModel : ViewModel(), KoinComponent {
	private val apiClient: ApiClient by inject()
	private val navigationRepository: NavigationRepository by inject()
	private val imageHelper: ImageHelper by inject()

	private val _uiState = MutableStateFlow(SeasonUiState())
	val uiState: StateFlow<SeasonUiState> = _uiState.asStateFlow()

	fun loadSeriesSeasons(seriesId: UUID) {
		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isLoading = true, error = null)
			
			try {
				// Get series details
				val seriesResponse = apiClient.itemsApi.getItems(
					ids = setOf(seriesId),
					fields = ItemRepository.itemFields,
				)
				
				val series = seriesResponse.content.items.firstOrNull()
				if (series == null) {
					_uiState.value = _uiState.value.copy(
						isLoading = false,
						error = "TV Show not found",
					)
					return@launch
				}

				// Get all seasons for this series
				val seasonsResponse = apiClient.itemsApi.getItems(
					parentId = seriesId,
					includeItemTypes = setOf(BaseItemKind.SEASON),
					fields = ItemRepository.itemFields,
					sortBy = setOf(ItemSortBy.SORT_NAME, ItemSortBy.INDEX_NUMBER),
					sortOrder = setOf(SortOrder.ASCENDING),
					enableUserData = true,
				)
				
				val seasons: List<BaseItemDto> = seasonsResponse.content.items
				
				val sections = mutableListOf<ImmersiveListSection>()
				
				// All Seasons section - main content
				if (seasons.isNotEmpty()) {
					sections.add(
						ImmersiveListSection(
							title = "Seasons",
							items = seasons,
							layout = ImmersiveListLayout.HORIZONTAL_CARDS,
						),
					)
				}

				_uiState.value = _uiState.value.copy(
					isLoading = false,
					sections = sections,
					series = series, // Store the series info
					title = series.name ?: "TV Show",
					seriesName = series.name,
				)
				
				Timber.d(
					"Loaded TV show '${series.name}' with ${seasons.size} seasons",
				)
			} catch (e: ApiClientException) {
				Timber.e(e, "API error loading season data")
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					error = "Failed to load season data: ${e.message}",
				)
			} catch (e: IllegalArgumentException) {
				Timber.e(e, "Invalid argument when loading season data")
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					error = "Invalid season data",
				)
			}
		}
	}

	fun onItemClick(item: BaseItemDto) {
		Timber.d("Season clicked: ${item.name} (${item.type})")
		when (item.type) {
			BaseItemKind.SEASON -> {
				// Navigate to episodes view for this season
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
			BaseItemKind.SERIES -> {
				// Navigate to series overview
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
			else -> {
				// Handle any other item types
				Timber.d("Navigating to item details for type: ${item.type}")
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
		}
	}

	fun onItemFocused(item: BaseItemDto?) {
		_uiState.value = _uiState.value.copy(focusedItem = item)
		
		// Log focus changes for debugging
		item?.let { focusedItem ->
			when (focusedItem.type) {
				BaseItemKind.SEASON -> {
					val seasonNumber = focusedItem.indexNumber
					val episodeCount = focusedItem.childCount
					
					Timber.d(
						"Focused on season: ${focusedItem.name} " +
							"(Season #$seasonNumber, $episodeCount episodes)",
					)
				}
				else -> {
					Timber.d("Focused on item: ${focusedItem.name} (${focusedItem.type})")
				}
			}
		}
	}

	fun getItemImageUrl(item: BaseItemDto): String? {
		return when (item.type) {
			BaseItemKind.EPISODE -> {
				// For episodes, prefer their own primary image (episode thumbnail)
				try {
					imageHelper.getPrimaryImageUrl(item, null, 400)
				} catch (e: IllegalArgumentException) {
					Timber.e(e, "Invalid argument when getting image for episode: ${item.name}")
					// Fallback to series image if episode doesn't have one
					getItemBackdropUrl(item)
				} catch (e: ApiClientException) {
					Timber.e(e, "API error getting image for episode: ${item.name}")
					// Fallback to series image if episode doesn't have one
					getItemBackdropUrl(item)
				}
			}
			else -> {
				try {
					imageHelper.getPrimaryImageUrl(item, null, 400)
				} catch (e: IllegalArgumentException) {
					Timber.e(e, "Invalid argument when getting image for item: ${item.name}")
					null
				} catch (e: ApiClientException) {
					Timber.e(e, "API error getting image for item: ${item.name}")
					null
				}
			}
		}
	}
	
	fun getItemBackdropUrl(item: BaseItemDto): String? {
		return try {
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

	fun getItemLogoUrl(item: BaseItemDto): String? {
		return try {
			imageHelper.getLogoImageUrl(item, 400)
		} catch (e: IllegalArgumentException) {
			Timber.e(e, "Failed to get logo for item: ${item.name}")
			null
		}
	}
}

/**
 * UI State for the TV Show seasons screen
 */
data class SeasonUiState(
	val isLoading: Boolean = true,
	val sections: List<ImmersiveListSection> = emptyList(),
	val error: String? = null,
	val focusedItem: BaseItemDto? = null,
	val series: BaseItemDto? = null,
	val title: String = "TV Show",
	val seriesName: String? = null,
)
