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

				// Get episodes for this season with better sorting and metadata
				val episodesResponse = apiClient.itemsApi.getItems(
					parentId = seasonId,
					includeItemTypes = setOf(BaseItemKind.EPISODE),
					fields = ItemRepository.itemFields,
					sortBy = setOf(ItemSortBy.SORT_NAME, ItemSortBy.PRODUCTION_YEAR),
					sortOrder = setOf(SortOrder.ASCENDING),
					enableUserData = true, // Ensure we get watch status and progress
				)
				
				val episodes: List<BaseItemDto> = episodesResponse.content.items
				
				// Get series information if available
				var seriesName: String? = null
				if (season.seriesId != null) {
					try {
						val seriesResponse = apiClient.itemsApi.getItems(
							ids = setOf(season.seriesId!!),
							fields = ItemRepository.itemFields,
						)
						seriesName = seriesResponse.content.items.firstOrNull()?.name
					} catch (e: ApiClientException) {
						Timber.w(e, "Failed to load series information")
					}
				}
				
				val sections = mutableListOf<ImmersiveListSection>()
				
				// Continue Watching section (episodes with progress) - Show first if any
				val continueWatching: List<BaseItemDto> = episodes.filter { episode ->
					val progress = episode.userData?.playedPercentage ?: 0.0
					progress > 0.0 && progress < 90.0 // Started but not finished
				}
				
				if (continueWatching.isNotEmpty()) {
					sections.add(
						ImmersiveListSection(
							title = "Continue Watching",
							items = continueWatching,
							layout = ImmersiveListLayout.HORIZONTAL_CARDS,
						),
					)
				}
				
				// All Episodes section - main content
				if (episodes.isNotEmpty()) {
					sections.add(
						ImmersiveListSection(
							title = "Episodes",
							items = episodes,
							layout = ImmersiveListLayout.HORIZONTAL_CARDS,
						),
					)
				}
				
				// Recently Added episodes (last 10 by date added)
				val recentEpisodes = episodes
					.filter { it.dateCreated != null }
					.sortedByDescending { it.dateCreated }
					.take(10)
					
				if (recentEpisodes.isNotEmpty() && recentEpisodes.size > 1) {
					sections.add(
						ImmersiveListSection(
							title = "Recently Added",
							items = recentEpisodes,
							layout = ImmersiveListLayout.HORIZONTAL_CARDS,
						),
					)
				}

				_uiState.value = _uiState.value.copy(
					isLoading = false,
					sections = sections,
					season = season,
					title = season.name ?: "Season",
					seriesName = seriesName,
				)
				
				Timber.d(
					"Loaded season '${season.name}' with ${episodes.size} episodes. " +
						"Continue watching: ${continueWatching.size}, Recently added: ${recentEpisodes.size}",
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
		Timber.d("Episode clicked: ${item.name} (${item.type})")
		when (item.type) {
			BaseItemKind.EPISODE -> {
				// For episodes, navigate to detailed episode view with play options
				// This will show episode details, cast info, and play buttons
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
			BaseItemKind.SERIES -> {
				// Navigate back to series overview
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
			BaseItemKind.SEASON -> {
				// Navigate to this season (shouldn't happen in season view, but handle it)
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
					val progress = getEpisodeProgress(focusedItem)
					val runtime = getEpisodeRuntime(focusedItem)
					
					Timber.d(
						"Focused on episode: ${focusedItem.name} " +
							"(#$episodeNumber, ${if (runtime != null) "$runtime, " else ""}${progress.toInt()}% watched)",
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
	
	/**
	 * Get formatted episode title with episode number
	 */
	fun getEpisodeDisplayTitle(episode: BaseItemDto): String {
		val episodeNumber = episode.indexNumber
		val episodeName = episode.name ?: "Episode"
		
		return if (episodeNumber != null) {
			"$episodeNumber. $episodeName"
		} else {
			episodeName
		}
	}
	
	/**
	 * Get formatted episode runtime
	 */
	fun getEpisodeRuntime(episode: BaseItemDto): String? {
		val ticks = episode.runTimeTicks
		if (ticks == null || ticks == 0L) return null
		
		val totalMinutes = (ticks / 10_000_000L) / 60L
		val hours = totalMinutes / 60
		val minutes = totalMinutes % 60
		
		return if (hours > 0) {
			"${hours}h ${minutes}m"
		} else {
			"${minutes}m"
		}
	}
	
	/**
	 * Get episode progress percentage for display
	 */
	fun getEpisodeProgress(episode: BaseItemDto): Double {
		return episode.userData?.playedPercentage ?: 0.0
	}
	
	/**
	 * Check if episode has been watched
	 */
	fun isEpisodeWatched(episode: BaseItemDto): Boolean {
		return episode.userData?.played == true
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
