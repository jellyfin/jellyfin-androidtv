package org.jellyfin.androidtv.ui.jellyseerr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.repository.JellyseerrMovieDetails
import org.jellyfin.androidtv.data.repository.JellyseerrRepository
import org.jellyfin.androidtv.data.repository.JellyseerrRequest
import org.jellyfin.androidtv.data.repository.JellyseerrSearchItem
import org.jellyfin.androidtv.data.repository.JellyseerrPersonDetails
import org.jellyfin.androidtv.data.repository.JellyseerrCast

enum class JellyseerrDiscoverCategory(val titleResId: Int) {
	TRENDING(org.jellyfin.androidtv.R.string.jellyseerr_discover_title),
	POPULAR_MOVIES(org.jellyfin.androidtv.R.string.jellyseerr_popular_title),
	UPCOMING_MOVIES(org.jellyfin.androidtv.R.string.jellyseerr_upcoming_movies_title),
	POPULAR_TV(org.jellyfin.androidtv.R.string.jellyseerr_popular_tv_title),
	UPCOMING_TV(org.jellyfin.androidtv.R.string.jellyseerr_upcoming_tv_title),
}

data class JellyseerrUiState(
	val isLoading: Boolean = false,
	val query: String = "",
	val results: List<JellyseerrSearchItem> = emptyList(),
	val ownRequests: List<JellyseerrRequest> = emptyList(),
	val errorMessage: String? = null,
	val selectedItem: JellyseerrSearchItem? = null,
	val selectedMovie: JellyseerrMovieDetails? = null,
	val showAllTrendsGrid: Boolean = false,
	val requestStatusMessage: String? = null,
	val discoverCurrentPage: Int = 1,
	val discoverHasMore: Boolean = true,
	val selectedPerson: JellyseerrPersonDetails? = null,
	val personCredits: List<JellyseerrSearchItem> = emptyList(),
	val originDetailItem: JellyseerrSearchItem? = null,
	val popularResults: List<JellyseerrSearchItem> = emptyList(),
	val recentRequests: List<JellyseerrSearchItem> = emptyList(),
	val popularTvResults: List<JellyseerrSearchItem> = emptyList(),
	val upcomingMovieResults: List<JellyseerrSearchItem> = emptyList(),
	val upcomingTvResults: List<JellyseerrSearchItem> = emptyList(),
	val discoverCategory: JellyseerrDiscoverCategory = JellyseerrDiscoverCategory.TRENDING,
	val scrollPositions: Map<String, ScrollPosition> = emptyMap(),
	val lastFocusedItemId: Int? = null,
	val lastFocusedViewAllKey: String? = null,
)

data class ScrollPosition(
	val index: Int = 0,
	val offset: Int = 0,
)

class JellyseerrViewModel(
	private val repository: JellyseerrRepository,
) : ViewModel() {
	private val _uiState = MutableStateFlow(JellyseerrUiState())
	val uiState: StateFlow<JellyseerrUiState> = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			refreshOwnRequestsInternal()
			loadDiscoverInternal()
			loadPopular()
			loadPopularTv()
			loadUpcomingMovies()
			loadUpcomingTv()
			loadRecentRequests()
		}
	}

	private fun markItemsWithRequests(
		items: List<JellyseerrSearchItem>,
		requests: List<JellyseerrRequest>,
	): List<JellyseerrSearchItem> = items.map { item ->
		val match = requests.firstOrNull { it.tmdbId == item.id }
		val requestStatus = match?.status
		val hasPendingRequest = requestStatus != null && requestStatus != 5
		val availableFromRequest = requestStatus == 5

		item.copy(
			isRequested = hasPendingRequest,
			isAvailable = item.isAvailable || availableFromRequest,
			requestId = match?.id ?: item.requestId,
			requestStatus = requestStatus ?: item.requestStatus,
			isPartiallyAvailable = item.isPartiallyAvailable,
		)
	}

	private fun markSelectedItemWithRequests(
		selectedItem: JellyseerrSearchItem?,
		requests: List<JellyseerrRequest>,
	): JellyseerrSearchItem? {
		selectedItem ?: return null

		val match = requests.firstOrNull { it.tmdbId == selectedItem.id }
		val requestStatus = match?.status
		val hasPendingRequest = requestStatus != null && requestStatus != 5
		val availableFromRequest = requestStatus == 5

		return selectedItem.copy(
			isRequested = hasPendingRequest,
			isAvailable = selectedItem.isAvailable || availableFromRequest,
			requestId = match?.id ?: selectedItem.requestId,
			requestStatus = requestStatus ?: selectedItem.requestStatus,
			isPartiallyAvailable = selectedItem.isPartiallyAvailable,
		)
	}

	fun updateQuery(query: String) {
		_uiState.update { it.copy(query = query) }
	}

	fun updateLastFocusedItem(itemId: Int?) {
		_uiState.update { it.copy(lastFocusedItemId = itemId, lastFocusedViewAllKey = null) }
	}

	fun updateLastFocusedViewAll(key: String?) {
		_uiState.update { it.copy(lastFocusedViewAllKey = key, lastFocusedItemId = null) }
	}

	fun search() {
		val term = _uiState.value.query.trim()
		if (term.isBlank()) return

		viewModelScope.launch {
			_uiState.update { it.copy(isLoading = true, errorMessage = null) }

			val searchResult = repository.search(term)

			if (searchResult.isFailure) {
				val error = searchResult.exceptionOrNull()
				_uiState.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
					)
				}
				return@launch
			}

			val results = searchResult.getOrThrow()
			val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
			val currentRequests = _uiState.value.ownRequests
			val marked = markItemsWithRequests(resultsWithAvailability, currentRequests)

			_uiState.update {
				it.copy(
					isLoading = false,
					results = marked,
				)
			}
		}
	}

	fun refreshOwnRequests() {
		viewModelScope.launch {
			refreshOwnRequestsInternal()
		}
	}

	private suspend fun loadPopular() {
		_uiState.update { it.copy(isLoading = true, errorMessage = null) }

		val currentRequests = _uiState.value.ownRequests

		val popularResult = repository.discoverMovies(page = 1)

		if (popularResult.isFailure) {
			val error = popularResult.exceptionOrNull()
			_uiState.update {
				it.copy(
					isLoading = false,
					errorMessage = error?.message,
				)
			}
			return
		}

		val results = popularResult.getOrThrow()
		val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
		val marked = markItemsWithRequests(resultsWithAvailability, currentRequests)

		_uiState.update {
			it.copy(
				isLoading = false,
				popularResults = marked,
			)
		}
	}

	private suspend fun loadPopularTv() {
		_uiState.update { it.copy(isLoading = true, errorMessage = null) }

		val currentRequests = _uiState.value.ownRequests

		val popularResult = repository.discoverTv(page = 1)

		if (popularResult.isFailure) {
			val error = popularResult.exceptionOrNull()
			_uiState.update {
				it.copy(
					isLoading = false,
					errorMessage = error?.message,
				)
			}
			return
		}

		val results = popularResult.getOrThrow()
		val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
		val marked = markItemsWithRequests(resultsWithAvailability, currentRequests)

		_uiState.update {
			it.copy(
				isLoading = false,
				popularTvResults = marked,
			)
		}
	}

	private suspend fun loadUpcomingMovies() {
		_uiState.update { it.copy(isLoading = true, errorMessage = null) }

		val currentRequests = _uiState.value.ownRequests

		val upcomingResult = repository.discoverUpcomingMovies(page = 1)

		if (upcomingResult.isFailure) {
			val error = upcomingResult.exceptionOrNull()
			_uiState.update {
				it.copy(
					isLoading = false,
					errorMessage = error?.message,
				)
			}
			return
		}

		val results = upcomingResult.getOrThrow()
		val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
		val marked = markItemsWithRequests(resultsWithAvailability, currentRequests)

		_uiState.update {
			it.copy(
				isLoading = false,
				upcomingMovieResults = marked,
			)
		}
	}

	private suspend fun loadUpcomingTv() {
		_uiState.update { it.copy(isLoading = true, errorMessage = null) }

		val currentRequests = _uiState.value.ownRequests

		val upcomingResult = repository.discoverUpcomingTv(page = 1)

		if (upcomingResult.isFailure) {
			val error = upcomingResult.exceptionOrNull()
			_uiState.update {
				it.copy(
					isLoading = false,
					errorMessage = error?.message,
				)
			}
			return
		}

		val results = upcomingResult.getOrThrow()
		val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
		val marked = markItemsWithRequests(resultsWithAvailability, currentRequests)

		_uiState.update {
			it.copy(
				isLoading = false,
				upcomingTvResults = marked,
			)
		}
	}

	private suspend fun loadRecentRequests() {
		val result = repository.getRecentRequests()

		if (result.isFailure) {
			val error = result.exceptionOrNull()
			_uiState.update {
				it.copy(errorMessage = error?.message)
			}
			return
		}

		val requests = result.getOrThrow()
		// Lade Poster und Verfügbarkeit von Jellyfin wie bei den anderen Kategorien
		val requestsWithAvailability = repository.markAvailableInJellyfin(requests).getOrElse { requests }

		_uiState.update {
			it.copy(recentRequests = requestsWithAvailability)
		}
	}

	private suspend fun refreshOwnRequestsInternal() {
		val result = repository.getOwnRequests()

		if (result.isFailure) {
			val error = result.exceptionOrNull()
			_uiState.update {
				it.copy(errorMessage = error?.message)
			}
			return
		}

		val requests = result.getOrThrow()

		_uiState.update { state ->
			val updatedResults = markItemsWithRequests(state.results, requests)
			val updatedPopular = markItemsWithRequests(state.popularResults, requests)
			val updatedPopularTv = markItemsWithRequests(state.popularTvResults, requests)
			val updatedUpcomingMovies = markItemsWithRequests(state.upcomingMovieResults, requests)
			val updatedUpcomingTv = markItemsWithRequests(state.upcomingTvResults, requests)
			val updatedSelectedItem = markSelectedItemWithRequests(state.selectedItem, requests)

			state.copy(
				ownRequests = requests,
				results = updatedResults,
				popularResults = updatedPopular,
				popularTvResults = updatedPopularTv,
				upcomingMovieResults = updatedUpcomingMovies,
				upcomingTvResults = updatedUpcomingTv,
				selectedItem = updatedSelectedItem ?: state.selectedItem,
			)
		}
	}

	private suspend fun loadDiscoverInternal() {
		_uiState.update { it.copy(isLoading = true, errorMessage = null) }

		val currentRequests = _uiState.value.ownRequests

		val discoverResult = repository.discoverTrending()

		if (discoverResult.isFailure) {
			val error = discoverResult.exceptionOrNull()
			_uiState.update {
				it.copy(
					isLoading = false,
					errorMessage = error?.message,
				)
			}
			return
		}

		val results = discoverResult.getOrThrow()
		val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
		val marked = markItemsWithRequests(resultsWithAvailability, currentRequests)

		_uiState.update {
			it.copy(
				isLoading = false,
				results = marked,
				discoverCurrentPage = 1,
				discoverHasMore = results.isNotEmpty(),
				discoverCategory = JellyseerrDiscoverCategory.TRENDING,
			)
		}
	}

	fun showAllTrends() {
		viewModelScope.launch {
			_uiState.update {
				it.copy(
					showAllTrendsGrid = true,
					isLoading = true,
					errorMessage = null,
					lastFocusedItemId = null,
				)
			}

			val currentRequests = _uiState.value.ownRequests

			val discoverResult = repository.discoverTrending(page = 1)

			if (discoverResult.isFailure) {
				val error = discoverResult.exceptionOrNull()
				_uiState.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
						showAllTrendsGrid = true,
						lastFocusedItemId = null,
					)
				}
				return@launch
			}

			val results = discoverResult.getOrThrow()
			val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
			val marked = markItemsWithRequests(resultsWithAvailability, currentRequests)

			_uiState.update {
				it.copy(
					isLoading = false,
					results = marked,
					discoverCurrentPage = 1,
					discoverHasMore = results.isNotEmpty(),
					discoverCategory = JellyseerrDiscoverCategory.TRENDING,
				)
			}
		}
	}

	fun showAllPopularMovies() {
		viewModelScope.launch {
			_uiState.update {
				it.copy(
					showAllTrendsGrid = true,
					isLoading = true,
					errorMessage = null,
					lastFocusedItemId = null,
				)
			}

			val currentRequests = _uiState.value.ownRequests

			val discoverResult = repository.discoverMovies(page = 1)

			if (discoverResult.isFailure) {
				val error = discoverResult.exceptionOrNull()
				_uiState.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
						showAllTrendsGrid = true,
						lastFocusedItemId = null,
					)
				}
				return@launch
			}

			val results = discoverResult.getOrThrow()
			val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
			val marked = markItemsWithRequests(resultsWithAvailability, currentRequests)

			_uiState.update {
				it.copy(
					isLoading = false,
					results = marked,
					discoverCurrentPage = 1,
					discoverHasMore = results.isNotEmpty(),
					discoverCategory = JellyseerrDiscoverCategory.POPULAR_MOVIES,
				)
			}
		}
	}

	fun showAllUpcomingMovies() {
		viewModelScope.launch {
			_uiState.update {
				it.copy(
					showAllTrendsGrid = true,
					isLoading = true,
					errorMessage = null,
					lastFocusedItemId = null,
				)
			}

			val currentRequests = _uiState.value.ownRequests

			val discoverResult = repository.discoverUpcomingMovies(page = 1)

			if (discoverResult.isFailure) {
				val error = discoverResult.exceptionOrNull()
				_uiState.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
						showAllTrendsGrid = true,
					)
				}
				return@launch
			}

			val results = discoverResult.getOrThrow()
			val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
			val marked = markItemsWithRequests(resultsWithAvailability, currentRequests)

			_uiState.update {
				it.copy(
					isLoading = false,
					results = marked,
					discoverCurrentPage = 1,
					discoverHasMore = results.isNotEmpty(),
					discoverCategory = JellyseerrDiscoverCategory.UPCOMING_MOVIES,
				)
			}
		}
	}

	fun showAllPopularTv() {
		viewModelScope.launch {
			_uiState.update {
				it.copy(
					showAllTrendsGrid = true,
					isLoading = true,
					errorMessage = null,
					lastFocusedItemId = null,
				)
			}

			val currentRequests = _uiState.value.ownRequests

			val discoverResult = repository.discoverTv(page = 1)

			if (discoverResult.isFailure) {
				val error = discoverResult.exceptionOrNull()
				_uiState.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
						showAllTrendsGrid = true,
						lastFocusedItemId = null,
					)
				}
				return@launch
			}

			val results = discoverResult.getOrThrow()
			val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
			val marked = markItemsWithRequests(resultsWithAvailability, currentRequests)

			_uiState.update {
				it.copy(
					isLoading = false,
					results = marked,
					discoverCurrentPage = 1,
					discoverHasMore = results.isNotEmpty(),
					discoverCategory = JellyseerrDiscoverCategory.POPULAR_TV,
				)
			}
		}
	}

	fun showAllUpcomingTv() {
		viewModelScope.launch {
			_uiState.update {
				it.copy(
					showAllTrendsGrid = true,
					isLoading = true,
					errorMessage = null,
					lastFocusedItemId = null,
				)
			}

			val currentRequests = _uiState.value.ownRequests

			val discoverResult = repository.discoverUpcomingTv(page = 1)

			if (discoverResult.isFailure) {
				val error = discoverResult.exceptionOrNull()
				_uiState.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
						showAllTrendsGrid = true,
						lastFocusedItemId = null,
					)
				}
				return@launch
			}

			val results = discoverResult.getOrThrow()
			val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
			val marked = markItemsWithRequests(resultsWithAvailability, currentRequests)

			_uiState.update {
				it.copy(
					isLoading = false,
					results = marked,
					discoverCurrentPage = 1,
					discoverHasMore = results.isNotEmpty(),
					discoverCategory = JellyseerrDiscoverCategory.UPCOMING_TV,
				)
			}
		}
	}

	fun loadMoreTrends() {
		val state = _uiState.value
		// Lazy loading für alle Kategorien
		if (!state.showAllTrendsGrid || state.isLoading || !state.discoverHasMore) return

		val nextPage = state.discoverCurrentPage + 1

		viewModelScope.launch {
			_uiState.update { it.copy(isLoading = true, errorMessage = null) }

			val currentRequests = _uiState.value.ownRequests

			// Wähle die richtige API-Funktion basierend auf der Kategorie
			val discoverResult = when (state.discoverCategory) {
				JellyseerrDiscoverCategory.TRENDING -> repository.discoverTrending(page = nextPage)
				JellyseerrDiscoverCategory.POPULAR_MOVIES -> repository.discoverMovies(page = nextPage)
				JellyseerrDiscoverCategory.UPCOMING_MOVIES -> repository.discoverUpcomingMovies(page = nextPage)
				JellyseerrDiscoverCategory.POPULAR_TV -> repository.discoverTv(page = nextPage)
				JellyseerrDiscoverCategory.UPCOMING_TV -> repository.discoverUpcomingTv(page = nextPage)
			}

			if (discoverResult.isFailure) {
				val error = discoverResult.exceptionOrNull()
				_uiState.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
					)
				}
				return@launch
			}

			val results = discoverResult.getOrThrow()

			if (results.isEmpty()) {
				_uiState.update {
					it.copy(
						isLoading = false,
						discoverCurrentPage = nextPage,
						discoverHasMore = false,
					)
				}
			} else {
				val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
				val marked = markItemsWithRequests(resultsWithAvailability, currentRequests)

				_uiState.update {
					it.copy(
						isLoading = false,
						results = it.results + marked,
						discoverCurrentPage = nextPage,
						discoverHasMore = true,
					)
				}
			}
		}
	}

	fun closeAllTrends() {
		_uiState.update { it.copy(showAllTrendsGrid = false) }
		// Refresh data when returning to main menu to sync with Jellyseerr server
		viewModelScope.launch {
			refreshOwnRequestsInternal()
			loadRecentRequests()
		}
	}

	fun request(item: JellyseerrSearchItem, seasons: List<Int>? = null) {
		if (seasons.isNullOrEmpty() && item.isRequested) return

		viewModelScope.launch {
			_uiState.update { it.copy(errorMessage = null, requestStatusMessage = null) }

			repository.createRequest(item, seasons)
				.onSuccess {
					if (!seasons.isNullOrEmpty()) {
						markSelectedSeasonsAsRequested(item, seasons)
					}
					// Reload own requests und markiere Suchergebnisse
					refreshOwnRequests()
					// Refresh current details to update season status immediately
					refreshCurrentDetails()
					_uiState.update {
						it.copy(requestStatusMessage = "Anfrage gesendet")
					}
				}
				.onFailure { error ->
					_uiState.update {
						it.copy(errorMessage = error.message, requestStatusMessage = "Anfrage fehlgeschlagen")
					}
				}
		}
	}

	private fun markSelectedSeasonsAsRequested(item: JellyseerrSearchItem, seasons: List<Int>) {
		if (seasons.isEmpty()) return

		val requestedSeasonNumbers = seasons.toSet()

		_uiState.update { state ->
			val updatedMovie = state.selectedMovie?.copy(
				seasons = state.selectedMovie.seasons.map { season ->
					if (season.seasonNumber in requestedSeasonNumbers) season.copy(status = 1) else season
				},
			)

			val updatedItem = state.selectedItem?.takeIf { it.id == item.id }?.copy(
				isRequested = true,
				requestStatus = 1,
			)

			state.copy(
				selectedMovie = updatedMovie ?: state.selectedMovie,
				selectedItem = updatedItem ?: state.selectedItem,
			)
		}
	}

	fun showDetailsForItem(item: JellyseerrSearchItem) {
		_uiState.update {
			it.copy(
				isLoading = true,
				errorMessage = null,
				selectedItem = item,
				selectedMovie = null,
				// Personen-Ansicht schließen, damit Details im Vordergrund sind
				selectedPerson = null,
				personCredits = emptyList(),
			)
		}

		viewModelScope.launch {
			val result = when (item.mediaType) {
				"movie" -> repository.getMovieDetails(item.id)
				"tv" -> repository.getTvDetails(item.id)
				else -> {
					_uiState.update { it.copy(isLoading = false) }
					return@launch
				}
			}

			result
				.onSuccess { details ->
					// Für TV-Serien prüfen ob teilweise verfügbar
					val updatedItem = if (item.mediaType == "tv") {
						val seasons = details.seasons.filter { it.seasonNumber > 0 }
						// Zähle nur Staffeln mit status = 5 (Available)
						val availableSeasons = seasons.count { it.status == 5 }
						val totalSeasons = seasons.size

						val isPartiallyAvailable = availableSeasons > 0 && availableSeasons < totalSeasons
						val isFullyAvailable = availableSeasons == totalSeasons && totalSeasons > 0

						item.copy(
							isPartiallyAvailable = isPartiallyAvailable,
							isAvailable = isFullyAvailable
						)
					} else {
						item
					}

					_uiState.update {
						it.copy(
							isLoading = false,
							selectedMovie = details,
							selectedItem = updatedItem,
						)
					}
				}
				.onFailure { error ->
					_uiState.update {
						it.copy(
							isLoading = false,
							errorMessage = error.message,
						)
					}
				}
		}
	}


	fun showDetailsForItemFromPerson(item: JellyseerrSearchItem) {
		_uiState.update {
			it.copy(
				isLoading = true,
				errorMessage = null,
				selectedItem = item,
				selectedMovie = null,
				// selectedPerson und personCredits bleiben gesetzt
			)
		}

		viewModelScope.launch {
			val result = when (item.mediaType) {
				"movie" -> repository.getMovieDetails(item.id)
				"tv" -> repository.getTvDetails(item.id)
				else -> {
					_uiState.update { it.copy(isLoading = false) }
					return@launch
				}
			}

			result
				.onSuccess { details ->
					_uiState.update {
						it.copy(
							isLoading = false,
							selectedMovie = details,
						)
					}
				}
				.onFailure { error ->
					_uiState.update {
						it.copy(
							isLoading = false,
							errorMessage = error.message,
						)
					}
				}
		}
	}


	fun showDetailsForRequest(request: JellyseerrRequest) {
		val tmdbId = request.tmdbId ?: return

				_uiState.update {
					val hasPendingRequest = request.status != null && request.status != 5

					it.copy(
						isLoading = true,
						errorMessage = null,
						selectedItem = JellyseerrSearchItem(
							id = tmdbId,
							mediaType = request.mediaType ?: "movie",
							title = request.title,
							overview = null,
							isRequested = hasPendingRequest,
							isAvailable = request.status == 5,
							requestStatus = request.status,
							requestId = request.id,
						),
						selectedMovie = null,
					)
				}

		viewModelScope.launch {
			val mediaType = request.mediaType ?: "movie"

			val result = when (mediaType) {
				"movie" -> repository.getMovieDetails(tmdbId)
				"tv" -> repository.getTvDetails(tmdbId)
				else -> {
					_uiState.update { it.copy(isLoading = false) }
					return@launch
				}
			}

			result
				.onSuccess { details ->
					_uiState.update {
						it.copy(
							isLoading = false,
							selectedMovie = details,
						)
					}
				}
				.onFailure { error ->
					_uiState.update {
						it.copy(
							isLoading = false,
							errorMessage = error.message,
						)
					}
				}
		}
	}

	fun refreshCurrentDetails() {
		val currentItem = _uiState.value.selectedItem ?: return

		viewModelScope.launch {
			val result = when (currentItem.mediaType) {
				"movie" -> repository.getMovieDetails(currentItem.id)
				"tv" -> repository.getTvDetails(currentItem.id)
				else -> return@launch
			}

			result.onSuccess { details ->
				// Für TV-Serien prüfen ob teilweise verfügbar
				val updatedItem = if (currentItem.mediaType == "tv") {
					val seasons = details.seasons.filter { it.seasonNumber > 0 }
					val availableSeasons = seasons.count { it.status == 5 }
					val totalSeasons = seasons.size

					val isPartiallyAvailable = availableSeasons > 0 && availableSeasons < totalSeasons
					val isFullyAvailable = availableSeasons == totalSeasons && totalSeasons > 0

					currentItem.copy(
						isPartiallyAvailable = isPartiallyAvailable,
						isAvailable = isFullyAvailable
					)
				} else {
					currentItem
				}

				_uiState.update {
					it.copy(
						selectedMovie = details,
						selectedItem = updatedItem,
					)
				}
			}
		}
	}

	fun closeDetails() {
		_uiState.update {
			it.copy(
				selectedItem = null,
				selectedMovie = null,
				requestStatusMessage = null,
			)
		}
		// Refresh data when returning to main menu to sync with Jellyseerr server
		viewModelScope.launch {
			refreshOwnRequestsInternal()
			loadRecentRequests()
		}
	}

	fun saveScrollPosition(key: String, index: Int, offset: Int) {
		_uiState.update {
			val newPositions = it.scrollPositions.toMutableMap()
			newPositions[key] = ScrollPosition(index, offset)
			it.copy(scrollPositions = newPositions)
		}
	}

	fun clearRequestStatus() {
		_uiState.update { it.copy(requestStatusMessage = null) }
	}

	fun showPerson(person: JellyseerrCast) {
		viewModelScope.launch {
			val current = _uiState.value
			val origin = current.originDetailItem ?: current.selectedItem

			_uiState.update {
				it.copy(
					isLoading = true,
					errorMessage = null,
					originDetailItem = origin,
				)
			}

			val detailsResult = repository.getPersonDetails(person.id)
			val creditsResult = repository.getPersonCredits(person.id)

			if (detailsResult.isFailure || creditsResult.isFailure) {
				val error = detailsResult.exceptionOrNull() ?: creditsResult.exceptionOrNull()
				_uiState.update {
					it.copy(isLoading = false, errorMessage = error?.message)
				}
				return@launch
			}

			val details = detailsResult.getOrThrow()
			val credits = creditsResult.getOrThrow()
			val withAvailability = repository.markAvailableInJellyfin(credits).getOrElse { credits }
			val marked = markItemsWithRequests(withAvailability, _uiState.value.ownRequests)

			_uiState.update {
				it.copy(
					isLoading = false,
					selectedPerson = details,
					personCredits = marked,
					// Detailansicht ausblenden, Person-Ansicht im Vordergrund
					selectedItem = null,
					selectedMovie = null,
				)
			}
		}
	}


	fun closePerson() {
		val origin = _uiState.value.originDetailItem

		if (origin != null) {
			// Person schließen und danach zum ursprünglichen Film zurückkehren
			_uiState.update {
				it.copy(
					selectedPerson = null,
					personCredits = emptyList(),
					originDetailItem = null,
				)
			}

			showDetailsForItem(origin)
		} else {
			_uiState.update {
				it.copy(
					selectedPerson = null,
					personCredits = emptyList(),
				)
			}
			// Refresh data when returning to main menu to sync with Jellyseerr server
			viewModelScope.launch {
				refreshOwnRequestsInternal()
				loadRecentRequests()
			}
		}
	}
}
