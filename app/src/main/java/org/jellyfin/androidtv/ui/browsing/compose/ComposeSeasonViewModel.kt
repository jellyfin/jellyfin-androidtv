package org.jellyfin.androidtv.ui.browsing.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.composable.tv.ImmersiveListSection
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
 * ViewModel for the Season detail screen showing episodes
 * Displays episodes for a specific season using the immersive list pattern
 */
class ComposeSeasonViewModel : ViewModel(), KoinComponent {
	private val apiClient: ApiClient by inject()
	private val navigationRepository: NavigationRepository by inject()
	private val imageHelper: ImageHelper by inject()

	private val _uiState = MutableStateFlow(SeasonUiState())
	val uiState: StateFlow<SeasonUiState> = _uiState.asStateFlow()

	fun loadSeasonData(seasonId: UUID) {
		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isLoading = true, error = null)
			
			try {
				// Get season details
				val seasonResponse = apiClient.itemsApi.getItem(
					itemId = seasonId,
					userId = apiClient.userId!!,
				)
				
				val season = seasonResponse.content
				if (season == null) {
					_uiState.value = _uiState.value.copy(
						isLoading = false,
						error = "Season not found",
					)
					return@launch
				}

				// Get episodes for this season
				val episodesResponse = apiClient.tvShowsApi.getEpisodes(
					seriesId = season.seriesId ?: seasonId,
					seasonId = seasonId,
					userId = apiClient.userId!!,
					sortBy = listOf(ItemSortBy.SORT_NAME),
					sortOrder = listOf(SortOrder.ASCENDING),
					enableUserData = true,
					enableImages = true,
				)
				
				val episodes = episodesResponse.content?.items ?: emptyList()
				
				val sections = mutableListOf<ImmersiveListSection>()
				
				// All Episodes section
				if (episodes.isNotEmpty()) {
					sections.add(
						ImmersiveListSection(
							title = "Episodes",
							items = episodes,
							layout = ImmersiveListSection.Layout.HORIZONTAL_CARDS,
						),
					)
				}
				
				// Continue Watching section (episodes with progress)
				val continueWatching = episodes.filter { episode ->
					val progress = episode.userData?.playedPercentage ?: 0.0
					progress > 0.0 && progress < 90.0 // Started but not finished
				}
				
				if (continueWatching.isNotEmpty()) {
					sections.add(
						0, // Add at the beginning
						ImmersiveListSection(
							title = "Continue Watching",
							items = continueWatching,
							layout = ImmersiveListSection.Layout.HORIZONTAL_CARDS,
						),
					)
				}

				_uiState.value = _uiState.value.copy(
					isLoading = false,
					sections = sections,
					season = season,
					title = season.name ?: "Season",
					seriesName = season.seriesName,
				)
				
				Timber.d("Loaded ${episodes.size} episodes for season: ${season.name}")
			} catch (e: Exception) {
				Timber.e(e, "Error loading season data")
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					error = "Failed to load season data: ${e.message}",
				)
			}
		}
	}

	fun onItemClick(item: BaseItemDto) {
		Timber.d("Episode clicked: ${item.name}")
		when (item.type) {
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
