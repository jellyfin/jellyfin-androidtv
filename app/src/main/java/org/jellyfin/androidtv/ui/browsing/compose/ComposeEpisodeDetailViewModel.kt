package org.jellyfin.androidtv.ui.browsing.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.domain.usecase.PlayItemUseCase
import org.jellyfin.androidtv.domain.usecase.ToggleWatchedStatusUseCase
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient. όταν.url
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.UUID

data class EpisodeDetailUiState(
    val isLoading: Boolean = true,
    val episode: BaseItemDto? = null,
    val seriesName: String? = null,
    val seasonName: String? = null,
    val error: String? = null
)

class ComposeEpisodeDetailViewModel : ViewModel(), KoinComponent {

    private val apiClient: ApiClient by inject()
    private val itemRepository: ItemRepository by inject()
    private val navigationRepository: NavigationRepository by inject()
    private val imageHelper: ImageHelper by inject()
    private val playItemUseCase: PlayItemUseCase by inject()
    private val toggleWatchedStatusUseCase: ToggleWatchedStatusUseCase by inject()

    private val _uiState = MutableStateFlow(EpisodeDetailUiState())
    val uiState: StateFlow<EpisodeDetailUiState> = _uiState.asStateFlow()

    fun loadEpisodeDetails(episodeId: UUID) {
        viewModelScope.launch {
            _uiState.value = EpisodeDetailUiState(isLoading = true)
            try {
                val episode = itemRepository.getItem(episodeId)
                if (episode == null) {
                    _uiState.value = EpisodeDetailUiState(isLoading = false, error = "Episode not found")
                    return@launch
                }

                var seriesName: String? = episode.seriesName
                var seasonName: String? = episode.seasonName

                // If series or season name is missing, try to fetch parent items
                if (episode.type == BaseItemKind.EPISODE) {
                    if (seriesName.isNullOrEmpty() && episode.seriesId != null) {
                        itemRepository.getItem(episode.seriesId!!)?.let { series ->
                            seriesName = series.name
                        }
                    }
                    if (seasonName.isNullOrEmpty() && episode.seasonId != null) {
                        itemRepository.getItem(episode.seasonId!!)?.let { season ->
                            seasonName = season.name
                        }
                    }
                }

                _uiState.value = EpisodeDetailUiState(
                    isLoading = false,
                    episode = episode,
                    seriesName = seriesName,
                    seasonName = seasonName
                )
            } catch (e: ApiClientException) {
                Timber.e(e, "Error loading episode details for ID: $episodeId")
                _uiState.value = EpisodeDetailUiState(isLoading = false, error = e.message ?: "API client error")
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error loading episode details for ID: $episodeId")
                _uiState.value = EpisodeDetailUiState(isLoading = false, error = e.message ?: "Unexpected error")
            }
        }
    }

    fun playEpisode(episode: BaseItemDto) {
        viewModelScope.launch {
            playItemUseCase(episode.id, 0L, false) // TODO: Check start position and resume status
        }
    }

    fun toggleWatchedStatus(episode: BaseItemDto) {
        viewModelScope.launch {
            try {
                val newWatchedStatus = !(episode.userData?.played ?: false)
                val success = if (newWatchedStatus) {
                    toggleWatchedStatusUseCase.markAsWatched(episode.id)
                } else {
                    toggleWatchedStatusUseCase.markAsUnwatched(episode.id)
                }

                if (success) {
                    // Refresh item to update UI
                    loadEpisodeDetails(episode.id)
                } else {
                    Timber.w("Failed to toggle watched status for episode ${episode.id}")
                    // Optionally show an error to the user
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling watched status for episode ${episode.id}")
                // Optionally show an error to the user
            }
        }
    }

    fun getItemBackdropUrl(item: BaseItemDto?): String? {
        item ?: return null
        try {
            // Prioritize item's own backdrop
            val itemBackdrop = item.itemBackdropImages.firstOrNull()?.getUrl(
                api = apiClient,
                maxWidth = 1920,
                maxHeight = 1080
            )
            if (itemBackdrop != null) return itemBackdrop

            // Fallback for episodes: try season backdrop
            if (item.type == BaseItemKind.EPISODE && item.seasonId != null) {
                // This requires fetching the season item if not already available.
                // For simplicity here, we assume season/series backdrops are less critical
                // or would be part of a more complex pre-fetched data structure.
                // A direct call here could be slow or introduce complexity.
                // Let's rely on the episode's own backdrop primarily for this screen.
                // If uiState.episode.season.backdrop is available, use that.
            }
            // Further fallback: try series backdrop
            if (item.type == BaseItemKind.EPISODE && item.seriesId != null) {
                 // Similar to season, fetching series item just for backdrop might be too much here.
            }

            // If no specific backdrop, try primary image if it's landscape-ish (though less ideal)
            // For now, let's stick to designated backdrops.
            return null
        } catch (e: ApiClientException) {
            Timber.e(e, "Failed to get backdrop for item: ${item.name}")
            return null
        }
    }

    fun getItemLogoUrl(item: BaseItemDto?): String? {
        item ?: return null
        return try {
            // Prefer series logo for episodes
            if (item.type == BaseItemKind.EPISODE && item.seriesId != null) {
                // This would ideally fetch the series item and get its logo.
                // To avoid another fetch, we rely on ImageHelper which might have logic for this.
                // If ImageHelper cannot find series logo from episode item, it might return episode's own logo (if any) or series logo if item.seriesPrimaryImageTag is good.
                 imageHelper.getLogoImageUrl(item, maxHeight = 200) // Max height for logo
            } else {
                imageHelper.getLogoImageUrl(item, maxHeight = 200)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get logo for item: ${item.name}")
            null
        }
    }
}
