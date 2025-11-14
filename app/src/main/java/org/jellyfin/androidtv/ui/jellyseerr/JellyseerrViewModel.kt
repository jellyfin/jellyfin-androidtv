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
)

class JellyseerrViewModel(
	private val repository: JellyseerrRepository,
) : ViewModel() {
	private val _uiState = MutableStateFlow(JellyseerrUiState())
	val uiState: StateFlow<JellyseerrUiState> = _uiState.asStateFlow()

	init {
		refreshOwnRequests()
		loadDiscover()
	}

	fun updateQuery(query: String) {
		_uiState.update { it.copy(query = query) }
	}

	fun search() {
		val term = _uiState.value.query.trim()
		if (term.isBlank()) return

		viewModelScope.launch {
			_uiState.update { it.copy(isLoading = true, errorMessage = null) }

			repository.search(term)
				.onSuccess { results ->
					val currentRequests = _uiState.value.ownRequests
					val marked = results.map { item ->
						val requested = currentRequests.any { it.tmdbId == item.id }
						item.copy(isRequested = requested)
					}
					_uiState.update {
						it.copy(
							isLoading = false,
							results = marked,
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

	fun refreshOwnRequests() {
		viewModelScope.launch {
			repository.getOwnRequests()
				.onSuccess { requests ->
					_uiState.update { state ->
						val updatedResults = state.results.map { item ->
							val requested = requests.any { it.tmdbId == item.id }
							item.copy(isRequested = requested)
						}
						state.copy(
							ownRequests = requests,
							results = updatedResults,
						)
					}
				}
				.onFailure { error ->
					_uiState.update {
						it.copy(errorMessage = error.message)
					}
				}
		}
	}

	private fun loadDiscover() {
		viewModelScope.launch {
			_uiState.update { it.copy(isLoading = true, errorMessage = null) }

			val currentRequests = _uiState.value.ownRequests

			repository.discoverMovies()
				.onSuccess { results ->
					val marked = results.map { item ->
						val requested = currentRequests.any { it.tmdbId == item.id }
						item.copy(isRequested = requested)
					}
					_uiState.update {
						it.copy(
							isLoading = false,
							results = marked,
							discoverCurrentPage = 1,
							discoverHasMore = results.isNotEmpty(),
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

	fun showAllTrends() {
		viewModelScope.launch {
			_uiState.update { it.copy(showAllTrendsGrid = true, isLoading = true, errorMessage = null) }

			val currentRequests = _uiState.value.ownRequests

			repository.discoverMovies(page = 1)
				.onSuccess { results ->
					val marked = results.map { item ->
						val requested = currentRequests.any { it.tmdbId == item.id }
						item.copy(isRequested = requested)
					}

					_uiState.update {
						it.copy(
							isLoading = false,
							results = marked,
							discoverCurrentPage = 1,
							discoverHasMore = results.isNotEmpty(),
						)
					}
				}
				.onFailure { error ->
					_uiState.update {
						it.copy(
							isLoading = false,
							errorMessage = error.message,
							showAllTrendsGrid = true,
						)
					}
				}
		}
	}

	fun loadMoreTrends() {
		val state = _uiState.value
		if (!state.showAllTrendsGrid || state.isLoading || !state.discoverHasMore) return

		val nextPage = state.discoverCurrentPage + 1

		viewModelScope.launch {
			_uiState.update { it.copy(isLoading = true, errorMessage = null) }

			val currentRequests = _uiState.value.ownRequests

			repository.discoverMovies(page = nextPage)
				.onSuccess { results ->
					if (results.isEmpty()) {
						_uiState.update {
							it.copy(
								isLoading = false,
								discoverCurrentPage = nextPage,
								discoverHasMore = false,
							)
						}
					} else {
						val marked = results.map { item ->
							val requested = currentRequests.any { it.tmdbId == item.id }
							item.copy(isRequested = requested)
						}

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

	fun closeAllTrends() {
		_uiState.update { it.copy(showAllTrendsGrid = false) }
	}

	fun request(item: JellyseerrSearchItem) {
		if (item.isRequested) return

		viewModelScope.launch {
			_uiState.update { it.copy(errorMessage = null, requestStatusMessage = null) }

			repository.createRequest(item)
				.onSuccess {
					// Reload own requests und markiere Suchergebnisse
					refreshOwnRequests()
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

	fun showDetailsForItem(item: JellyseerrSearchItem) {
		_uiState.update { it.copy(isLoading = true, errorMessage = null, selectedItem = item, selectedMovie = null) }

		if (item.mediaType != "movie") {
			// For now only movie details are loaded from Jellyseerr; for TV we just show basic info.
			_uiState.update { it.copy(isLoading = false) }
			return
		}

		viewModelScope.launch {
			repository.getMovieDetails(item.id)
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
			it.copy(
				isLoading = true,
				errorMessage = null,
				selectedItem = JellyseerrSearchItem(
					id = tmdbId,
					mediaType = request.mediaType ?: "movie",
					title = request.title,
					overview = null,
				),
				selectedMovie = null,
			)
		}

		viewModelScope.launch {
			repository.getMovieDetails(tmdbId)
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

	fun closeDetails() {
		_uiState.update {
			it.copy(
				selectedItem = null,
				selectedMovie = null,
				requestStatusMessage = null,
			)
		}
	}

	fun clearRequestStatus() {
		_uiState.update { it.copy(requestStatusMessage = null) }
	}

}
