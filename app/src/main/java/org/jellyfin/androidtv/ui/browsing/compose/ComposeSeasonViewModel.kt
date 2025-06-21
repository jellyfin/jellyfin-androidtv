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
 * ViewModel for the Season detail screen
 * Displays all episodes for a specific season
 */
class ComposeSeasonViewModel : ViewModel(), KoinComponent {
	private val apiClient: ApiClient by inject()
	private val navigationRepository: NavigationRepository by inject()
	private val imageHelper: ImageHelper by inject()

	private val _uiState = MutableStateFlow(SeasonUiState())
	val uiState: StateFlow<SeasonUiState> = _uiState.asStateFlow()

	fun loadSeasonEpisodes(seasonId: UUID) {
		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isLoading = true, error = null)
			
			try {
				// Get season details
				val seasonResponse = apiClient.itemsApi.getItems(
					ids = setOf(seasonId),
					fields = ItemRepository.itemFields,
				)
				
				val season = seasonResponse.content.items.firstOrNull()
				if (season == null) {
					_uiState.value = _uiState.value.copy(
						isLoading = false,
						error = "Season not found",
					)
					return@launch
				}

				// Get all episodes for this season
				val episodesResponse = apiClient.itemsApi.getItems(
					parentId = seasonId,
					includeItemTypes = setOf(BaseItemKind.EPISODE),
					fields = ItemRepository.itemFields,
					sortBy = setOf(ItemSortBy.INDEX_NUMBER),
					sortOrder = setOf(SortOrder.ASCENDING),
					enableUserData = true,
				)
				
				val episodes: List<BaseItemDto> = episodesResponse.content.items
				
				val sections = mutableListOf<ImmersiveListSection>()
				
				// Episodes section - main content
				if (episodes.isNotEmpty()) {
					sections.add(
						ImmersiveListSection(
							title = "Episodes",
							items = episodes,
							layout = ImmersiveListLayout.HORIZONTAL_CARDS,
							cardAspectRatio = 4f / 3f,
						),
					)
				}

				_uiState.value = _uiState.value.copy(
					isLoading = false,
					sections = sections,
					season = season, // Store the season info
					title = season.name ?: "Season",
					seriesName = season.seriesName,
				)
				
				Timber.d(
					"Loaded season '${season.name}' with ${episodes.size} episodes",
				)
			} catch (e: ApiClientException) {
				Timber.e(e, "API error loading episode data")
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					error = "Failed to load episode data: ${e.message}",
				)
			} catch (e: IllegalArgumentException) {
				Timber.e(e, "Invalid argument when loading episode data")
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					error = "Invalid episode data",
				)
			}
		}
	}

	fun onItemClick(item: BaseItemDto) {
		Timber.d("Episode clicked: ${item.name} (${item.type})")
		when (item.type) {
			BaseItemKind.EPISODE -> {
				// Navigate to episode detail view
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
				BaseItemKind.EPISODE -> {
					val episodeNumber = focusedItem.indexNumber
					val seasonNumber = focusedItem.parentIndexNumber
					
					Timber.d(
						"Focused on episode: ${focusedItem.name} " +
							"(Season $seasonNumber, Episode $episodeNumber)",
					)
				}
				else -> {
					Timber.d("Focused on item: ${focusedItem.name} (${focusedItem.type})")
				}
			}
		}
	}

	// Test helper function to simulate error state
	internal fun simulateError(errorMessage: String) {
		_uiState.value = _uiState.value.copy(
			isLoading = false,
			error = errorMessage,
		)
	}

	// Test helper function to simulate loading state
	internal fun simulateLoading() {
		_uiState.value = _uiState.value.copy(
			isLoading = true,
			error = null,
		)
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
 * UI State for the Season detail screen
 */
data class SeasonUiState(
	val isLoading: Boolean = true,
	val sections: List<ImmersiveListSection> = emptyList(),
	val error: String? = null,
	val focusedItem: BaseItemDto? = null,
	val season: BaseItemDto? = null,
	val title: String = "Season",
	val seriesName: String? = null,
)
