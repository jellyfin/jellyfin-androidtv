package org.jellyfin.androidtv.ui.home.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.seriesPrimaryImage
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest
import timber.log.Timber

data class HomeScreenState(
	val isLoading: Boolean = true,
	val libraries: List<BaseItemDto> = emptyList(),
	val resumeItems: List<BaseItemDto> = emptyList(),
	val nextUpItems: List<BaseItemDto> = emptyList(),
	val latestMovies: List<BaseItemDto> = emptyList(),
	val latestEpisodes: List<BaseItemDto> = emptyList(),
	val recentlyAddedStuff: List<BaseItemDto> = emptyList(),
	val error: String? = null,
)

/**
 * Enhanced ViewModel for Compose Home Screen with real data loading
 */
class SimpleHomeViewModel(
	private val userRepository: UserRepository,
	private val userViewsRepository: UserViewsRepository,
	private val navigationRepository: NavigationRepository,
	private val apiClient: ApiClient,
	private val imageHelper: ImageHelper,
) : ViewModel() {

	private val _homeState = MutableStateFlow(HomeScreenState())
	val homeState: StateFlow<HomeScreenState> = _homeState.asStateFlow()

	init {
		loadHomeData()
	}

	private fun loadHomeData() {
		viewModelScope.launch {
			try {
				Timber.d("SimpleHomeViewModel: Starting to load home data")
				_homeState.value = _homeState.value.copy(isLoading = true, error = null)
				// Load data concurrently
				val userViewsDeferred = async { userViewsRepository.views.first() }
				val resumeItemsDeferred = async { loadResumeItems() }
				val nextUpItemsDeferred = async { loadNextUpItems() }
				val latestMoviesDeferred = async { loadLatestMovies() }
				val latestEpisodesDeferred = async { loadLatestEpisodes() }
				val recentlyAddedStuffDeferred = async { loadRecentlyAddedStuff() }

				val userViews = userViewsDeferred.await()
				val resumeItems = resumeItemsDeferred.await()
				val nextUpItems = nextUpItemsDeferred.await()
				val latestMovies = latestMoviesDeferred.await()
				val latestEpisodes = latestEpisodesDeferred.await()
				val recentlyAddedStuff = recentlyAddedStuffDeferred.await()

				Timber.d(
					"SimpleHomeViewModel: Loaded ${userViews.size} user views, ${resumeItems.size} resume items, ${nextUpItems.size} next up items, ${latestMovies.size} latest movies, ${latestEpisodes.size} latest episodes, ${recentlyAddedStuff.size} recently added stuff",
				)

				_homeState.value = HomeScreenState(
					isLoading = false,
					libraries = userViews.toList(),
					resumeItems = resumeItems,
					nextUpItems = nextUpItems,
					latestMovies = latestMovies,
					latestEpisodes = latestEpisodes,
					recentlyAddedStuff = recentlyAddedStuff,
					error = null,
				)
			} catch (e: ApiClientException) {
				Timber.e(e, "Failed to load home data")
				_homeState.value = _homeState.value.copy(
					isLoading = false,
					error = e.message ?: "Unknown error occurred",
				)
			}
		}
	}

	private suspend fun loadResumeItems(): List<BaseItemDto> {
		return try {
			val request = GetResumeItemsRequest(
				limit = 10,
				mediaTypes = listOf(MediaType.VIDEO),
				includeItemTypes = listOf(BaseItemKind.EPISODE, BaseItemKind.MOVIE),
				enableTotalRecordCount = true,
				enableImages = true,
				excludeActiveSessions = true,
			)
			apiClient.itemsApi.getResumeItems(request = request).content.items.orEmpty()
		} catch (e: ApiClientException) {
			Timber.e(e, "Failed to load resume items")
			emptyList()
		}
	}

	private suspend fun loadNextUpItems(): List<BaseItemDto> {
		return try {
			val request = GetNextUpRequest(
				limit = 10,
				enableTotalRecordCount = true,
				disableFirstEpisode = false,
				enableResumable = false,
				enableRewatching = false,
			)
			apiClient.tvShowsApi.getNextUp(request = request).content.items.orEmpty()
		} catch (e: ApiClientException) {
			Timber.e(e, "Failed to load next up items")
			emptyList()
		}
	}

	private suspend fun loadLatestMovies(): List<BaseItemDto> {
		return try {
			val request = GetLatestMediaRequest(
				includeItemTypes = listOf(BaseItemKind.MOVIE),
				isPlayed = false,
				limit = 20,
				groupItems = false,
			)
			val result = apiClient.userLibraryApi.getLatestMedia(request = request).content
			Timber.d("Latest movies loaded: ${result.size} items")
			result
		} catch (e: ApiClientException) {
			Timber.e(e, "Failed to load latest movies")
			emptyList()
		}
	}

	private suspend fun loadLatestEpisodes(): List<BaseItemDto> {
		return try {
			val request = GetLatestMediaRequest(
				includeItemTypes = listOf(BaseItemKind.EPISODE),
				isPlayed = false,
				limit = 20,
				groupItems = false,
			)
			val result = apiClient.userLibraryApi.getLatestMedia(request = request).content
			Timber.d("Latest episodes loaded: ${result.size} items")
			result
		} catch (e: ApiClientException) {
			Timber.e(e, "Failed to load latest episodes")
			emptyList()
		}
	}

	private suspend fun loadRecentlyAddedStuff(): List<BaseItemDto> {
		return try {
			// First, find the "stuff" library
			val userViews = userViewsRepository.views.first()
			val stuffLibrary = userViews.find { it.name?.lowercase() == "stuff" }
			
			if (stuffLibrary != null) {
				val request = GetLatestMediaRequest(
					parentId = stuffLibrary.id,
					includeItemTypes = listOf(BaseItemKind.VIDEO),
					isPlayed = false,
					limit = 20,
					groupItems = false,
				)
				val result = apiClient.userLibraryApi.getLatestMedia(request = request).content
				Timber.d("Recently added stuff loaded: ${result.size} items")
				result
			} else {
				Timber.d("No 'stuff' library found")
				emptyList()
			}
		} catch (e: ApiClientException) {
			Timber.e(e, "Failed to load recently added stuff")
			emptyList()
		}
	}

	fun getItemImageUrl(item: BaseItemDto): String? {
		return try {
			val imageUrl = imageHelper.getPrimaryImageUrl(item, 300, 450)
			Timber.d("Generated image URL for ${item.name}: $imageUrl")
			imageUrl
		} catch (e: IllegalArgumentException) {
			Timber.e(e, "Failed to get image URL for item: ${item.name}")
			null
		}
	}

	fun getSeriesImageUrl(item: BaseItemDto): String? {
		return try {
			// For episodes, get the series poster image
			when (item.type) {
				BaseItemKind.EPISODE -> {
					// Use the built-in seriesPrimaryImage from ImageHelper
					val seriesImageUrl = item.seriesPrimaryImage?.getUrl(
						api = apiClient,
						maxWidth = 300,
						maxHeight = 450,
					)
					Timber.d("Generated series image URL for episode ${item.name} -> ${item.seriesName}: $seriesImageUrl")
					seriesImageUrl ?: getItemImageUrl(item)
				}
				else -> {
					// For non-episodes, use regular image
					getItemImageUrl(item)
				}
			}
		} catch (e: IllegalArgumentException) {
			Timber.e(e, "Failed to get series image URL for item: ${item.name}")
			// Fallback to regular image
			getItemImageUrl(item)
		}
	}

	fun onLibraryClick(library: BaseItemDto) {
		try {
			navigationRepository.navigate(Destinations.libraryBrowser(library))
		} catch (e: IllegalArgumentException) {
			Timber.e(e, "Failed to navigate to library: ${library.name}")
		}
	}

	fun onItemClick(item: BaseItemDto) {
		try {
			navigationRepository.navigate(Destinations.itemDetails(item.id))
		} catch (e: IllegalArgumentException) {
			Timber.e(e, "Failed to navigate to item: ${item.name}")
		}
	}

	fun refresh() {
		loadHomeData()
	}
}
