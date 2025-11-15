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
	val detailFromPerson: Boolean = false,
	val originDetailItem: JellyseerrSearchItem? = null,
	val popularResults: List<JellyseerrSearchItem> = emptyList()
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
		}
	}

	private fun markItemsWithRequests(
		items: List<JellyseerrSearchItem>,
		requests: List<JellyseerrRequest>,
	): List<JellyseerrSearchItem> = items.map { item ->
		val match = requests.firstOrNull { it.tmdbId == item.id }
		val requested = match != null
		val availableFromRequest = match?.status == 5

		item.copy(
			isRequested = requested,
			isAvailable = item.isAvailable || availableFromRequest,
			requestId = match?.id ?: item.requestId,
		)
	}

	private fun markSelectedItemWithRequests(
		selectedItem: JellyseerrSearchItem?,
		requests: List<JellyseerrRequest>,
	): JellyseerrSearchItem? {
		selectedItem ?: return null

		val match = requests.firstOrNull { it.tmdbId == selectedItem.id }
		val requested = match != null
		val availableFromRequest = match?.status == 5

		return selectedItem.copy(
			isRequested = requested,
			isAvailable = selectedItem.isAvailable || availableFromRequest,
			requestId = match?.id ?: selectedItem.requestId,
		)
	}

	fun updateQuery(query: String) {
		_uiState.update { it.copy(query = query) }
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

		val popularResult = repository.discoverMovies(page = 2)

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
			val updatedSelectedItem = markSelectedItemWithRequests(state.selectedItem, requests)

			state.copy(
				ownRequests = requests,
				results = updatedResults,
				selectedItem = updatedSelectedItem ?: state.selectedItem,
			)
		}
	}

	private suspend fun loadDiscoverInternal() {
		_uiState.update { it.copy(isLoading = true, errorMessage = null) }

		val currentRequests = _uiState.value.ownRequests

		val discoverResult = repository.discoverMovies()

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
			)
		}
	}

	fun showAllTrends() {
		viewModelScope.launch {
			_uiState.update { it.copy(showAllTrendsGrid = true, isLoading = true, errorMessage = null) }

			val currentRequests = _uiState.value.ownRequests

			val discoverResult = repository.discoverMovies(page = 1)

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
				)
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

			val discoverResult = repository.discoverMovies(page = nextPage)

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

	fun cancelRequest(item: JellyseerrSearchItem) {
		val requestId = item.requestId ?: return

		viewModelScope.launch {
			_uiState.update { it.copy(errorMessage = null, requestStatusMessage = null) }

			repository.cancelRequest(requestId)
				.onSuccess {
					refreshOwnRequests()
					_uiState.update {
						it.copy(requestStatusMessage = "Anfrage zurückgezogen")
					}
				}
				.onFailure { error ->
					_uiState.update {
						it.copy(errorMessage = error.message, requestStatusMessage = "Anfrage konnte nicht zurückgezogen werden")
					}
				}
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

		if (item.mediaType != "movie") {
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

		if (item.mediaType != "movie") {
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
					isRequested = true,
					isAvailable = request.status == 5,
					requestId = request.id,
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

	fun showPerson(person: JellyseerrCast) {
		viewModelScope.launch {
			_uiState.update { it.copy(isLoading = true, errorMessage = null) }

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
		_uiState.update {
			it.copy(
				selectedPerson = null,
				personCredits = emptyList(),
			)
		}
	}
}
