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
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.TimerInfoDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

/**
 * ViewModel for the Compose Live TV library screen with immersive list components
 */
class ComposeLiveTvViewModel : ViewModel(), KoinComponent {

	private val apiClient by inject<ApiClient>()
	private val itemRepository by inject<ItemRepository>()
	private val navigationRepository by inject<NavigationRepository>()
	private val backgroundService by inject<BackgroundService>()
	private val imageHelper by inject<ImageHelper>()

	private val _uiState = MutableStateFlow(LiveTvUiState())
	val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()

	private var currentFolder: BaseItemDto? = null

	fun loadLiveTvData(arguments: Bundle?) {
		viewModelScope.launch {
			try {
				_uiState.value = _uiState.value.copy(isLoading = true)

				// Parse folder from arguments (same as BrowseViewFragment)
				val folderJson = arguments?.getString(Extras.Folder)
				currentFolder = folderJson?.let {
					Json.decodeFromString(BaseItemDto.serializer(), it)
				}

				Timber.d("ComposeLiveTvViewModel: Loading data for folder: ${currentFolder?.name}")

				val sections = loadLiveTvSections()

				_uiState.value = _uiState.value.copy(
					isLoading = false,
					sections = sections,
					title = currentFolder?.name ?: "Live TV",
					folder = currentFolder,
				)

				Timber.d("ComposeLiveTvViewModel: Loaded ${sections.size} sections")
			} catch (e: Exception) {
				Timber.e(e, "Error loading Live TV data")
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					error = e.message,
				)
			}
		}
	}

	private suspend fun loadLiveTvSections(): List<ImmersiveListSection> {
		val sections = mutableListOf<ImmersiveListSection>()

		try {
			// On Now - Currently airing programs
			val onNowPrograms = loadOnNowPrograms()
			if (onNowPrograms.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "On Now",
						items = onNowPrograms,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
				Timber.d("Added On Now section with ${onNowPrograms.size} items")
			}

			// Coming Up - Upcoming programs
			val upcomingPrograms = loadUpcomingPrograms()
			if (upcomingPrograms.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Coming Up",
						items = upcomingPrograms,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
				Timber.d("Added Coming Up section with ${upcomingPrograms.size} items")
			}

			// Favorite Channels
			val favoriteChannels = loadFavoriteChannels()
			if (favoriteChannels.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Favorite Channels",
						items = favoriteChannels,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
				Timber.d("Added Favorite Channels section with ${favoriteChannels.size} items")
			}

			// Other Channels
			val otherChannels = loadOtherChannels()
			if (otherChannels.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Other Channels",
						items = otherChannels,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
				Timber.d("Added Other Channels section with ${otherChannels.size} items")
			}

			// Recent Recordings
			val recentRecordings = loadRecentRecordings()
			if (recentRecordings.isNotEmpty()) {
				sections.add(
					ImmersiveListSection(
						title = "Recent Recordings",
						items = recentRecordings,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
				Timber.d("Added Recent Recordings section with ${recentRecordings.size} items")
			}

			// Load smart sections (last 24 hours, scheduled, etc.)
			loadSmartSections(sections)
		} catch (e: Exception) {
			Timber.e(e, "Error loading Live TV sections")
		}

		return sections
	}

	private suspend fun loadOnNowPrograms(): List<BaseItemDto> {
		return try {
			val response = apiClient.liveTvApi.getRecommendedPrograms(
				isAiring = true,
				fields = ItemRepository.itemFields,
				imageTypeLimit = 1,
				enableTotalRecordCount = false,
				limit = 150,
			)
			response.content.items
		} catch (e: Exception) {
			Timber.e(e, "Error loading on now programs")
			emptyList()
		}
	}

	private suspend fun loadUpcomingPrograms(): List<BaseItemDto> {
		return try {
			val response = apiClient.liveTvApi.getRecommendedPrograms(
				isAiring = false,
				hasAired = false,
				fields = ItemRepository.itemFields,
				imageTypeLimit = 1,
				enableTotalRecordCount = false,
				limit = 150,
			)
			response.content.items
		} catch (e: Exception) {
			Timber.e(e, "Error loading upcoming programs")
			emptyList()
		}
	}

	private suspend fun loadFavoriteChannels(): List<BaseItemDto> {
		return try {
			val response = apiClient.liveTvApi.getLiveTvChannels(
				isFavorite = true,
			)
			response.content.items
		} catch (e: Exception) {
			Timber.e(e, "Error loading favorite channels")
			emptyList()
		}
	}

	private suspend fun loadOtherChannels(): List<BaseItemDto> {
		return try {
			val response = apiClient.liveTvApi.getLiveTvChannels(
				isFavorite = false,
			)
			response.content.items
		} catch (e: Exception) {
			Timber.e(e, "Error loading other channels")
			emptyList()
		}
	}

	private suspend fun loadRecentRecordings(): List<BaseItemDto> {
		return try {
			val response = apiClient.liveTvApi.getRecordings(
				fields = ItemRepository.itemFields,
				enableImages = true,
				limit = 50,
			)
			response.content.items
		} catch (e: Exception) {
			Timber.e(e, "Error loading recent recordings")
			emptyList()
		}
	}

	private suspend fun loadSmartSections(sections: MutableList<ImmersiveListSection>) {
		try {
			// Load recordings and timers for smart sections
			val recordingsResponse = apiClient.liveTvApi.getRecordings(
				fields = ItemRepository.itemFields,
				enableImages = true,
			)
			val timersResponse = apiClient.liveTvApi.getTimers()

			val recordings = recordingsResponse.content.items
			val timers = timersResponse.content.items

			// Process timers for scheduled recordings (next 24 hours)
			val next24Hours = LocalDateTime.now().plusDays(1)
			val scheduledRecordings = timers
				.filter { timer -> timer.startDate?.isBefore(next24Hours) == true }
				.mapNotNull { timer -> convertTimerToBaseItem(timer) }

			if (scheduledRecordings.isNotEmpty()) {
				sections.add(
					0,
					ImmersiveListSection(
						title = "Scheduled in Next 24 Hours",
						items = scheduledRecordings,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
			}

			// Process recordings for time-based sections
			val past24Hours = LocalDateTime.now().minusDays(1)
			val pastWeek = LocalDateTime.now().minusWeeks(1)

			val dayRecordings = recordings.filter {
				it.dateCreated?.isAfter(past24Hours) == true
			}
			val weekRecordings = recordings.filter {
				it.dateCreated?.isAfter(pastWeek) == true &&
					it.dateCreated?.isBefore(past24Hours) == true
			}

			if (dayRecordings.isNotEmpty()) {
				sections.add(
					0,
					ImmersiveListSection(
						title = "Past 24 Hours",
						items = dayRecordings,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
			}

			if (weekRecordings.isNotEmpty()) {
				sections.add(
					if (dayRecordings.isNotEmpty()) 1 else 0,
					ImmersiveListSection(
						title = "Past Week",
						items = weekRecordings,
						layout = ImmersiveListLayout.HORIZONTAL_CARDS,
					),
				)
			}
		} catch (e: Exception) {
			Timber.e(e, "Error loading smart sections")
		}
	}

	private fun convertTimerToBaseItem(timer: TimerInfoDto): BaseItemDto? {
		// Convert TimerInfoDto to BaseItemDto for display
		// This is a simplified conversion - in a real implementation you'd want
		// to create a proper BaseItemDto with all necessary fields
		return try {
			// Convert timer.id from String? to UUID - if null, generate a random UUID
			val id = timer.id?.let { UUID.fromString(it) } ?: UUID.randomUUID()
			BaseItemDto(
				id = id,
				name = timer.name,
				overview = timer.overview,
				startDate = timer.startDate,
				endDate = timer.endDate,
				channelId = timer.channelId,
				type = BaseItemKind.LIVE_TV_PROGRAM,
			)
		} catch (e: Exception) {
			Timber.e(e, "Error converting timer to BaseItemDto")
			null
		}
	}

	fun onItemClick(item: BaseItemDto) {
		Timber.d("Item clicked: ${item.name}")
		when (item.type) {
			BaseItemKind.LIVE_TV_PROGRAM -> {
				// Navigate to program details or start playback
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
			BaseItemKind.LIVE_TV_CHANNEL -> {
				// Navigate to channel or start playback
				navigationRepository.navigate(Destinations.itemDetails(item.id))
			}
			BaseItemKind.RECORDING -> {
				// Navigate to recording details or start playback
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
		// For Live TV items, prefer primary images
		return imageHelper.getPrimaryImageUrl(item, null, 400)
	}
	
	fun getItemBackdropUrl(item: BaseItemDto): String? {
		return item.itemBackdropImages.firstOrNull()?.getUrl(
			api = apiClient,
			maxWidth = 1920,
			maxHeight = 1080,
		)
	}

	fun getItemLogoUrl(item: BaseItemDto): String? {
		return try {
			// Use ImageHelper to get logo URL directly
			imageHelper.getLogoImageUrl(item, 400)
		} catch (e: Exception) {
			Timber.e(e, "Failed to get logo for item: ${item.name}")
			null
		}
	}
}

/**
 * UI State for the Live TV screen
 */
data class LiveTvUiState(
	val isLoading: Boolean = true,
	val sections: List<ImmersiveListSection> = emptyList(),
	val error: String? = null,
	val focusedItem: BaseItemDto? = null,
	val title: String = "Live TV",
	val folder: BaseItemDto? = null,
)
