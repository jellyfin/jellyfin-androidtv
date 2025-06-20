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
				val seriesResponse = apiClient.itemsApi.getItems(
					ids = setOf(seriesId),
					fields = ItemRepository.itemFields,
				)
				
				val series = seriesResponse.content.items.firstOrNull()
				if (series == null) {
					_uiState.value = _uiState.value.copy(
						isLoading = false,
						error = "Series not found",
					)
					return@launch
				}

				// Get seasons for this series
				val seasonsResponse = apiClient.itemsApi.getItems(
					parentId = seriesId,
					includeItemTypes = setOf(BaseItemKind.SEASON),
					fields = ItemRepository.itemFields,
					sortBy = setOf(ItemSortBy.SORT_NAME),
					sortOrder = setOf(SortOrder.ASCENDING),
				)
				
				val seasons: List<BaseItemDto> = seasonsResponse.content.items
				
				val sections = mutableListOf<ImmersiveListSection>()
				
				// Seasons section - horizontal cards
				if (seasons.isNotEmpty()) {
					sections.add(
						ImmersiveListSection(
							title = "Seasons",
							items = seasons,
							layout = ImmersiveListLayout.HORIZONTAL_CARDS,
						),
					)
				}

				// Cast section - horizontal row (using people from series data)
				val cast = series.people?.take(8) // Limit to 8 cast members for one row
				val castEmpty = cast.isNullOrEmpty()
				Timber.d("Found ${series.people?.size ?: 0} people for series: ${series.name}")
				if (!castEmpty) {
					// Convert BaseItemPerson to BaseItemDto for compatibility with ImmersiveListSection
					val castItems = cast.map { person ->
						// Create a minimal BaseItemDto representation for the person
						BaseItemDto(
							id = person.id,
							name = person.name,
							type = BaseItemKind.PERSON,
							overview = person.role,
							imageTags = person.primaryImageTag?.let { mapOf(org.jellyfin.sdk.model.api.ImageType.PRIMARY to it) },
							imageBlurHashes = person.imageBlurHashes,
						)
					}
					
					sections.add(
						ImmersiveListSection(
							title = "Cast",
							items = castItems,
							layout = ImmersiveListLayout.VERTICAL_GRID, // We'll use this for our horizontal row
						),
					)
					Timber.d("Added Cast section with ${castItems.size} members")
				}

				_uiState.value = _uiState.value.copy(
					isLoading = false,
					sections = sections,
					series = series,
					title = series.name ?: "Series",
					isCastEmpty = castEmpty,
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
				// For seasons in horizontal cards, prefer thumb/backdrop images for horizontal aspect ratio
				try {
					// First try to get thumb image (horizontal aspect ratio)
					val thumbUrl = imageHelper.getThumbImageUrl(item, 400, 225)
					if (thumbUrl != null) {
						return thumbUrl
					}
					
					// Fallback to backdrop image if available
					val backdropUrl = item.itemBackdropImages.firstOrNull()?.getUrl(
						api = apiClient,
						maxWidth = 400,
						maxHeight = 225,
					)
					if (backdropUrl != null) {
						return backdropUrl
					}
					
					// Final fallback to primary image (poster)
					imageHelper.getPrimaryImageUrl(item, null, 400)
				} catch (e: Exception) {
					Timber.e(e, "Failed to get image for season: ${item.name}")
					// Fallback to series backdrop or primary image
					getItemBackdropUrl(item) ?: imageHelper.getPrimaryImageUrl(item, null, 400)
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
			BaseItemKind.PERSON -> {
				// For cast/people, use their primary image (headshot)
				try {
					imageHelper.getPrimaryImageUrl(item, null, 400)
				} catch (e: Exception) {
					Timber.e(e, "Failed to get image for person: ${item.name}")
					null
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
	val isCastEmpty: Boolean = false,
)
