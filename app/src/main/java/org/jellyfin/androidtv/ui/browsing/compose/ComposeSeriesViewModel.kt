package org.jellyfin.androidtv.ui.browsing.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.composable.tv.ImmersiveListSection
import org.jellyfin.androidtv.ui.composable.tv.ImmersiveListLayout
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.UUID

/**
 * ViewModel for the Series detail screen showing seasons
 * Displays seasons for a specific TV series using the immersive list pattern
 */
class ComposeSeriesViewModel : ViewModel(), KoinComponent {
	private val apiClient: ApiClient by inject()
	private val navigationRepository: NavigationRepository by inject()
	private val imageHelper: ImageHelper by inject()

	private val _uiState = MutableStateFlow(SeriesUiState())
	val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

	fun loadSeriesData(seriesId: UUID) {
		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isLoading = true, error = null)
			
			try {
				// Get series details
				val seriesResponse = apiClient.itemsApi.getItem(
					itemId = seriesId,
					userId = apiClient.userId!!,
				)
				
				val series = seriesResponse.content
				if (series == null) {
					_uiState.value = _uiState.value.copy(
						isLoading = false,
						error = "Series not found",
					)
					return@launch
				}

				// Get seasons for this series
				val seasonsResponse = apiClient.tvShowsApi.getSeasons(
					seriesId = seriesId,
					userId = apiClient.userId!!,
					enableUserData = true,
					enableImages = true,
					sortBy = listOf(ItemSortBy.SORT_NAME),
					sortOrder = listOf(SortOrder.ASCENDING),
				)
				
				val seasons = seasonsResponse.content?.items ?: emptyList()
				
				val sections = mutableListOf<ImmersiveListSection>()
				
				// All Seasons section
				if (seasons.isNotEmpty()) {
					sections.add(
						ImmersiveListSection(
							title = "Seasons",
							items = seasons,
							layout = ImmersiveListLayout.HORIZONTAL_CARDS,
						),
					)
				}
				
				// Continue Watching section (seasons with progress)
				val continueWatching = seasons.filter { season ->
					val unplayedItemCount = season.userData?.unplayedItemCount ?: 0
					val totalItems = season.childCount ?: 0
					// Season has some progress if not all episodes are unwatched
					totalItems > 0 && unplayedItemCount < totalItems && unplayedItemCount > 0
				}
				
				if (continueWatching.isNotEmpty()) {
					sections.add(
						0, // Add at the beginning
						ImmersiveListSection(
							title = "Continue Watching",
							items = continueWatching,
							layout = ImmersiveListLayout.HORIZONTAL_CARDS,
						),
					)
				}

				// Recent Episodes section - get latest episodes across all seasons
				try {
					val recentEpisodesResponse = apiClient.tvShowsApi.getEpisodes(
						seriesId = seriesId,
						userId = apiClient.userId!!,
						sortBy = listOf(ItemSortBy.DATE_ADDED),
						sortOrder = listOf(SortOrder.DESCENDING),
						limit = 20,
						enableUserData = true,
						enableImages = true,
					)
					
					val recentEpisodes = recentEpisodesResponse.content?.items ?: emptyList()
					if (recentEpisodes.isNotEmpty()) {
						sections.add(
							ImmersiveListSection(
								title = "Recent Episodes",
								items = recentEpisodes.take(10), // Limit to 10 most recent
								layout = ImmersiveListLayout.HORIZONTAL_CARDS,
							),
						)
					}
				} catch (e: Exception) {
					Timber.w(e, "Failed to load recent episodes for series: ${series.name}")
				}

				_uiState.value = _uiState.value.copy(
					isLoading = false,
					sections = sections,
					series = series,
					title = series.name ?: "Series",
				)
				
				Timber.d("Loaded ${seasons.size} seasons for series: ${series.name}")
			} catch (e: Exception) {
				Timber.e(e, "Error loading series data")
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					error = "Failed to load series data: ${e.message}",
				)
			}
		}
	}

	fun onItemClick(item: BaseItemDto) {
		Timber.d("Item clicked: ${item.name} (Type: ${item.type})")
		when (item.type) {
			BaseItemKind.SEASON -> {
				// Navigate to Compose season detail screen (episodes view)
				navigationRepository.navigate(Destinations.composeSeasonDetail(item.id))
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

	fun onItemFocused(item: BaseItemDto?) {
		_uiState.value = _uiState.value.copy(focusedItem = item)
	}

	fun getItemImageUrl(item: BaseItemDto): String? {
		return when (item.type) {
			BaseItemKind.SEASON -> {
				// For seasons, prefer their primary image (season poster)
				try {
					imageHelper.getPrimaryImageUrl(item, null, 400)
				} catch (e: Exception) {
					Timber.e(e, "Failed to get image for season: ${item.name}")
					// Fallback to series image if season doesn't have one
					getItemBackdropUrl(item)
				}
			}
			BaseItemKind.EPISODE -> {
				// For episodes, prefer their own primary image (episode thumbnail)
				try {
					imageHelper.getPrimaryImageUrl(item, null, 400)
				} catch (e: Exception) {
					Timber.e(e, "Failed to get image for episode: ${item.name}")
					// Fallback to series image if episode doesn't have one
					getItemBackdropUrl(item)
				}
			}
			else -> {
				imageHelper.getPrimaryImageUrl(item, null, 400)
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
			Timber.e(e, "Failed to get backdrop for item: ${item.name}")
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
 * UI State for the Series detail screen
 */
data class SeriesUiState(
	val isLoading: Boolean = true,
	val sections: List<ImmersiveListSection> = emptyList(),
	val error: String? = null,
	val focusedItem: BaseItemDto? = null,
	val series: BaseItemDto? = null,
	val title: String = "Series",
)
