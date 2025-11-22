package org.jellyfin.androidtv.ui.jellyseerr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.repository.JellyseerrCast
import org.jellyfin.androidtv.data.repository.JellyseerrCompany
import org.jellyfin.androidtv.data.repository.JellyseerrGenreSlider
import org.jellyfin.androidtv.data.repository.JellyseerrRepository
import org.jellyfin.androidtv.data.repository.JellyseerrRequest
import org.jellyfin.androidtv.data.repository.JellyseerrSearchItem

class JellyseerrViewModel(
	private val repository: JellyseerrRepository,
) : ViewModel() {
	private val _uiState = MutableStateFlow(JellyseerrUiState())
	val uiState: StateFlow<JellyseerrUiState> = _uiState.asStateFlow()

	private val requestActions = JellyseerrRequestActions(repository, _uiState)
	private val discoveryActions = JellyseerrDiscoveryActions(repository, _uiState, viewModelScope, requestActions)
	private val detailActions = JellyseerrDetailActions(repository, _uiState, viewModelScope, requestActions) {
		discoveryActions.loadRecentRequests()
	}

	init {
		viewModelScope.launch {
			requestActions.refreshOwnRequests()
			discoveryActions.loadDiscover()
			discoveryActions.loadPopular()
			discoveryActions.loadPopularTv()
			discoveryActions.loadUpcomingMovies()
			discoveryActions.loadUpcomingTv()
			discoveryActions.loadRecentRequests()
			discoveryActions.loadMovieGenres()
			discoveryActions.loadTvGenres()
		}
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

	fun search(page: Int = 1) {
		discoveryActions.search(page)
	}

	fun loadSeasonEpisodes(tmdbId: Int, seasonNumber: Int) {
		detailActions.loadSeasonEpisodes(tmdbId, seasonNumber)
	}

	fun refreshOwnRequests() {
		viewModelScope.launch {
			requestActions.refreshOwnRequests()
		}
	}

	fun showAllTrends() {
		discoveryActions.showAllTrends()
	}

	fun showAllSearchResults() {
		discoveryActions.showAllSearchResults()
	}

	fun closeSearchResultsGrid() {
		discoveryActions.closeSearchResultsGrid()
	}

	fun showAllPopularMovies() {
		discoveryActions.showAllPopularMovies()
	}

	fun showAllUpcomingMovies() {
		discoveryActions.showAllUpcomingMovies()
	}

	fun showAllPopularTv() {
		discoveryActions.showAllPopularTv()
	}

	fun showAllUpcomingTv() {
		discoveryActions.showAllUpcomingTv()
	}

	fun showMovieGenre(genre: JellyseerrGenreSlider) {
		discoveryActions.showMovieGenre(genre)
	}

	fun showTvGenre(genre: JellyseerrGenreSlider) {
		discoveryActions.showTvGenre(genre)
	}

	fun showMovieStudio(company: JellyseerrCompany) {
		discoveryActions.showMovieStudio(company)
	}

	fun showTvNetwork(company: JellyseerrCompany) {
		discoveryActions.showTvNetwork(company)
	}

	fun loadMoreTrends() {
		discoveryActions.loadMoreTrends()
	}

	fun loadMoreSearchResults() {
		discoveryActions.loadMoreSearchResults()
	}

	fun closeAllTrends() {
		discoveryActions.closeAllTrends()
	}

	fun request(item: JellyseerrSearchItem, seasons: List<Int>? = null) {
		detailActions.request(item, seasons)
	}

	fun showDetailsForItem(item: JellyseerrSearchItem) {
		detailActions.showDetailsForItem(item)
	}

	fun showDetailsForItemFromPerson(item: JellyseerrSearchItem) {
		detailActions.showDetailsForItemFromPerson(item)
	}

	fun showDetailsForRequest(request: JellyseerrRequest) {
		detailActions.showDetailsForRequest(request)
	}

	fun refreshCurrentDetails() {
		detailActions.refreshCurrentDetails()
	}

	fun closeDetails() {
		detailActions.closeDetails()
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
		detailActions.showPerson(person)
	}

	fun showPersonFromSearchItem(item: JellyseerrSearchItem) {
		detailActions.showPersonFromSearchItem(item)
	}

	fun closePerson() {
		detailActions.closePerson()
	}
}
