package org.jellyfin.androidtv.ui.jellyseerr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.jellyfin.androidtv.data.repository.JellyseerrRepository

internal class JellyseerrRequestActions(
	private val repository: JellyseerrRepository,
	private val state: MutableStateFlow<JellyseerrUiState>,
) {
	suspend fun refreshOwnRequests() {
		val result = repository.getOwnRequests()

		if (result.isFailure) {
			val error = result.exceptionOrNull()
			state.update {
				it.copy(errorMessage = error?.message)
			}
			return
		}

		val requests = result.getOrThrow()

		state.update { current ->
			val updatedResults = JellyseerrRequestMarkers.markItemsWithRequests(current.results, requests)
			val updatedPopular = JellyseerrRequestMarkers.markItemsWithRequests(current.popularResults, requests)
			val updatedPopularTv = JellyseerrRequestMarkers.markItemsWithRequests(current.popularTvResults, requests)
			val updatedUpcomingMovies = JellyseerrRequestMarkers.markItemsWithRequests(current.upcomingMovieResults, requests)
			val updatedUpcomingTv = JellyseerrRequestMarkers.markItemsWithRequests(current.upcomingTvResults, requests)
			val updatedSelectedItem = JellyseerrRequestMarkers.markSelectedItemWithRequests(current.selectedItem, requests)

			current.copy(
				ownRequests = requests,
				results = updatedResults,
				popularResults = updatedPopular,
				popularTvResults = updatedPopularTv,
				upcomingMovieResults = updatedUpcomingMovies,
				upcomingTvResults = updatedUpcomingTv,
				selectedItem = updatedSelectedItem ?: current.selectedItem,
			)
		}
	}
}
