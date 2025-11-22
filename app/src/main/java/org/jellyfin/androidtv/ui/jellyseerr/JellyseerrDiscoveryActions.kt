package org.jellyfin.androidtv.ui.jellyseerr

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.repository.JellyseerrCompany
import org.jellyfin.androidtv.data.repository.JellyseerrCompanyDiscovery
import org.jellyfin.androidtv.data.repository.JellyseerrGenre
import org.jellyfin.androidtv.data.repository.JellyseerrGenreDiscovery
import org.jellyfin.androidtv.data.repository.JellyseerrGenreSlider
import org.jellyfin.androidtv.data.repository.JellyseerrRepository
import org.jellyfin.androidtv.data.repository.JellyseerrSearchItem

internal class JellyseerrDiscoveryActions(
	private val repository: JellyseerrRepository,
	private val state: MutableStateFlow<JellyseerrUiState>,
	private val scope: CoroutineScope,
	private val requestActions: JellyseerrRequestActions,
) {
	fun search(page: Int = 1) {
		val term = state.value.query.trim()
		if (term.isBlank()) {
			state.update {
				it.copy(
					showSearchResultsGrid = false,
					searchHasMore = false,
				)
			}
			return
		}

		scope.launch {
			if (page == 1) {
				state.update {
					it.copy(
						isLoading = true,
						errorMessage = null,
						showAllTrendsGrid = false,
						showSearchResultsGrid = false,
					)
				}
			}

			val searchResult = repository.search(term, page)

			if (searchResult.isFailure) {
				val error = searchResult.exceptionOrNull()
				state.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
						showSearchResultsGrid = false,
						searchHasMore = false,
					)
				}
				return@launch
			}

			val result = searchResult.getOrThrow()
			val resultsWithAvailability = repository.markAvailableInJellyfin(result.results).getOrElse { result.results }
			val currentRequests = state.value.ownRequests
			val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

			state.update { current ->
				val combined = if (page == 1) marked else current.results + marked
				current.copy(
					isLoading = false,
					results = combined,
					showSearchResultsGrid = true,
					showAllTrendsGrid = false,
					searchCurrentPage = page,
					searchTotalPages = result.totalPages,
					searchHasMore = page < result.totalPages,
				)
			}
		}
	}

	suspend fun loadDiscover() {
		state.update { it.copy(isLoading = true, errorMessage = null) }

		val currentRequests = state.value.ownRequests

		val discoverResult = repository.discoverTrending()

		if (discoverResult.isFailure) {
			val error = discoverResult.exceptionOrNull()
			state.update {
				it.copy(
					isLoading = false,
					errorMessage = error?.message,
				)
			}
			return
		}

		val results = discoverResult.getOrThrow()
		val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
		val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

		state.update {
			it.copy(
				isLoading = false,
				results = marked,
				trendingResults = marked,
				discoverCurrentPage = 1,
				discoverHasMore = results.isNotEmpty(),
				discoverCategory = JellyseerrDiscoverCategory.TRENDING,
			)
		}
	}

	suspend fun loadPopular() {
		state.update { it.copy(isLoading = true, errorMessage = null) }

		val currentRequests = state.value.ownRequests

		val popularResult = repository.discoverMovies(page = 1)

		if (popularResult.isFailure) {
			val error = popularResult.exceptionOrNull()
			state.update {
				it.copy(
					isLoading = false,
					errorMessage = error?.message,
				)
			}
			return
		}

		val results = popularResult.getOrThrow()
		val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
		val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

		state.update {
			it.copy(
				isLoading = false,
				popularResults = marked,
			)
		}
	}

	suspend fun loadPopularTv() {
		state.update { it.copy(isLoading = true, errorMessage = null) }

		val currentRequests = state.value.ownRequests

		val popularResult = repository.discoverTv(page = 1)

		if (popularResult.isFailure) {
			val error = popularResult.exceptionOrNull()
			state.update {
				it.copy(
					isLoading = false,
					errorMessage = error?.message,
				)
			}
			return
		}

		val results = popularResult.getOrThrow()
		val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
		val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

		state.update {
			it.copy(
				isLoading = false,
				popularTvResults = marked,
			)
		}
	}

	suspend fun loadUpcomingMovies() {
		state.update { it.copy(isLoading = true, errorMessage = null) }

		val currentRequests = state.value.ownRequests

		val upcomingResult = repository.discoverUpcomingMovies(page = 1)

		if (upcomingResult.isFailure) {
			val error = upcomingResult.exceptionOrNull()
			state.update {
				it.copy(
					isLoading = false,
					errorMessage = error?.message,
				)
			}
			return
		}

		val results = upcomingResult.getOrThrow()
		val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
		val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

		state.update {
			it.copy(
				isLoading = false,
				upcomingMovieResults = marked,
			)
		}
	}

	suspend fun loadUpcomingTv() {
		state.update { it.copy(isLoading = true, errorMessage = null) }

		val currentRequests = state.value.ownRequests

		val upcomingResult = repository.discoverUpcomingTv(page = 1)

		if (upcomingResult.isFailure) {
			val error = upcomingResult.exceptionOrNull()
			state.update {
				it.copy(
					isLoading = false,
					errorMessage = error?.message,
				)
			}
			return
		}

		val results = upcomingResult.getOrThrow()
		val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
		val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

		state.update {
			it.copy(
				isLoading = false,
				upcomingTvResults = marked,
			)
		}
	}

	suspend fun loadRecentRequests() {
		val result = repository.getRecentRequests()

		if (result.isFailure) {
			val error = result.exceptionOrNull()
			state.update {
				it.copy(errorMessage = error?.message)
			}
			return
		}

		val requests = result.getOrThrow()
		val requestsWithAvailability = repository.markAvailableInJellyfin(requests).getOrElse { requests }

		state.update {
			it.copy(recentRequests = requestsWithAvailability)
		}
	}

	fun showAllTrends() {
		scope.launch {
			state.update {
				it.copy(
					showAllTrendsGrid = true,
					showSearchResultsGrid = false,
					isLoading = true,
					errorMessage = null,
					lastFocusedItemId = null,
					discoverTitle = null,
					discoverGenre = null,
					discoverCompany = null,
					discoverGenreMediaType = null,
				)
			}

			val currentRequests = state.value.ownRequests

			val discoverResult = repository.discoverTrending(page = 1)

			if (discoverResult.isFailure) {
				val error = discoverResult.exceptionOrNull()
				state.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
						showAllTrendsGrid = true,
						showSearchResultsGrid = false,
						lastFocusedItemId = null,
					)
				}
				return@launch
			}

			val results = discoverResult.getOrThrow()
			val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
			val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

			state.update {
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

	fun showAllSearchResults() {
		state.update {
			it.copy(
				showSearchResultsGrid = true,
				showAllTrendsGrid = false,
			)
		}
	}

	fun closeSearchResultsGrid() {
		state.update {
			it.copy(
				showSearchResultsGrid = false,
				showAllTrendsGrid = false,
			)
		}
	}

	fun showAllPopularMovies() {
		scope.launch {
			state.update {
				it.copy(
					showAllTrendsGrid = true,
					showSearchResultsGrid = false,
					isLoading = true,
					errorMessage = null,
					lastFocusedItemId = null,
					discoverTitle = null,
					discoverGenre = null,
					discoverCompany = null,
					discoverGenreMediaType = null,
				)
			}

			val currentRequests = state.value.ownRequests

			val discoverResult = repository.discoverMovies(page = 1)

			if (discoverResult.isFailure) {
				val error = discoverResult.exceptionOrNull()
				state.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
						showAllTrendsGrid = true,
						showSearchResultsGrid = false,
						lastFocusedItemId = null,
					)
				}
				return@launch
			}

			val results = discoverResult.getOrThrow()
			val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
			val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

			state.update {
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
		scope.launch {
			state.update {
				it.copy(
					showAllTrendsGrid = true,
					showSearchResultsGrid = false,
					isLoading = true,
					errorMessage = null,
					lastFocusedItemId = null,
					discoverTitle = null,
					discoverGenre = null,
					discoverCompany = null,
					discoverGenreMediaType = null,
				)
			}

			val currentRequests = state.value.ownRequests

			val discoverResult = repository.discoverUpcomingMovies(page = 1)

			if (discoverResult.isFailure) {
				val error = discoverResult.exceptionOrNull()
				state.update {
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
			val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

			state.update {
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
		scope.launch {
			state.update {
				it.copy(
					showAllTrendsGrid = true,
					showSearchResultsGrid = false,
					isLoading = true,
					errorMessage = null,
					lastFocusedItemId = null,
					discoverTitle = null,
					discoverGenre = null,
					discoverCompany = null,
					discoverGenreMediaType = null,
				)
			}

			val currentRequests = state.value.ownRequests

			val discoverResult = repository.discoverTv(page = 1)

			if (discoverResult.isFailure) {
				val error = discoverResult.exceptionOrNull()
				state.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
						showAllTrendsGrid = true,
						showSearchResultsGrid = false,
						lastFocusedItemId = null,
					)
				}
				return@launch
			}

			val results = discoverResult.getOrThrow()
			val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
			val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

			state.update {
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
		scope.launch {
			state.update {
				it.copy(
					showAllTrendsGrid = true,
					showSearchResultsGrid = false,
					isLoading = true,
					errorMessage = null,
					lastFocusedItemId = null,
					discoverTitle = null,
					discoverGenre = null,
					discoverCompany = null,
					discoverGenreMediaType = null,
				)
			}

			val currentRequests = state.value.ownRequests

			val discoverResult = repository.discoverUpcomingTv(page = 1)

			if (discoverResult.isFailure) {
				val error = discoverResult.exceptionOrNull()
				state.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
						showAllTrendsGrid = true,
						showSearchResultsGrid = false,
						lastFocusedItemId = null,
					)
				}
				return@launch
			}

			val results = discoverResult.getOrThrow()
			val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
			val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

			state.update {
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

	fun showMovieGenre(genre: JellyseerrGenreSlider) {
		showGenre(genre, isTv = false)
	}

	fun showTvGenre(genre: JellyseerrGenreSlider) {
		showGenre(genre, isTv = true)
	}

	private fun showGenre(genre: JellyseerrGenreSlider, isTv: Boolean) {
		val category = if (isTv) JellyseerrDiscoverCategory.TV_GENRE else JellyseerrDiscoverCategory.MOVIE_GENRE

		scope.launch {
			state.update {
				it.copy(
					showAllTrendsGrid = true,
					showSearchResultsGrid = false,
					isLoading = true,
					errorMessage = null,
					lastFocusedItemId = null,
					discoverCategory = category,
					discoverTitle = genre.name,
					discoverGenre = JellyseerrGenre(genre.id, genre.name),
					discoverCompany = null,
					discoverGenreMediaType = if (isTv) "tv" else "movie",
				)
			}

			val currentRequests = state.value.ownRequests
			val discoveryResult = if (isTv) {
				repository.discoverTvByGenre(genre.id, page = 1)
			} else {
				repository.discoverMoviesByGenre(genre.id, page = 1)
			}

			if (discoveryResult.isFailure) {
				val error = discoveryResult.exceptionOrNull()
				state.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
					)
				}
				return@launch
			}

			val discovery = discoveryResult.getOrThrow()
			val marked = JellyseerrRequestMarkers.markItemsWithRequests(discovery.results, currentRequests)

			state.update {
				it.copy(
					isLoading = false,
					results = marked,
					discoverCurrentPage = 1,
					discoverHasMore = discovery.results.isNotEmpty(),
					discoverGenre = discovery.genre,
					discoverTitle = discovery.genre.name,
					discoverGenreMediaType = if (isTv) "tv" else "movie",
				)
			}
		}
	}

	fun showMovieStudio(company: JellyseerrCompany) {
		showCompany(company, isTv = false)
	}

	fun showTvNetwork(company: JellyseerrCompany) {
		showCompany(company, isTv = true)
	}

	private fun showCompany(company: JellyseerrCompany, isTv: Boolean) {
		val category = if (isTv) JellyseerrDiscoverCategory.TV_NETWORKS else JellyseerrDiscoverCategory.MOVIE_STUDIOS

		scope.launch {
			state.update {
				it.copy(
					showAllTrendsGrid = true,
					showSearchResultsGrid = false,
					isLoading = true,
					errorMessage = null,
					lastFocusedItemId = null,
					discoverCategory = category,
					discoverTitle = company.name,
					discoverGenre = null,
					discoverCompany = company,
					discoverGenreMediaType = if (isTv) "tv" else "movie",
				)
			}

			val currentRequests = state.value.ownRequests

			val discoveryResult = if (isTv) {
				repository.discoverTvByNetwork(company.id, page = 1)
			} else {
				repository.discoverMoviesByStudio(company.id, page = 1)
			}

			if (discoveryResult.isFailure) {
				val error = discoveryResult.exceptionOrNull()
				state.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
					)
				}
				return@launch
			}

			val discovery = discoveryResult.getOrThrow()
			val resultsWithAvailability = repository.markAvailableInJellyfin(discovery.results).getOrElse { discovery.results }
			val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

			state.update {
				it.copy(
					isLoading = false,
					results = marked,
					discoverCurrentPage = 1,
					discoverHasMore = discovery.results.isNotEmpty(),
					discoverGenre = null,
					discoverCompany = discovery.company,
					discoverTitle = discovery.company.name,
					discoverGenreMediaType = if (isTv) "tv" else "movie",
				)
			}
		}
	}

	fun loadMoreTrends() {
		val current = state.value
		if (!current.showAllTrendsGrid || current.isLoading || !current.discoverHasMore) return

		val nextPage = current.discoverCurrentPage + 1

		scope.launch {
			state.update { it.copy(isLoading = true, errorMessage = null) }

			val currentRequests = state.value.ownRequests

			if (
				current.discoverCategory == JellyseerrDiscoverCategory.MOVIE_GENRE ||
				current.discoverCategory == JellyseerrDiscoverCategory.TV_GENRE ||
				current.discoverCategory == JellyseerrDiscoverCategory.MOVIE_STUDIOS ||
				current.discoverCategory == JellyseerrDiscoverCategory.TV_NETWORKS
			) {
				val filterId = when (current.discoverCategory) {
					JellyseerrDiscoverCategory.MOVIE_GENRE,
					JellyseerrDiscoverCategory.TV_GENRE -> current.discoverGenre?.id
					JellyseerrDiscoverCategory.MOVIE_STUDIOS,
					JellyseerrDiscoverCategory.TV_NETWORKS -> current.discoverCompany?.id
					else -> null
				}

				if (filterId == null) {
					state.update { it.copy(isLoading = false) }
					return@launch
				}

				val discoveryResult = when (current.discoverCategory) {
					JellyseerrDiscoverCategory.MOVIE_GENRE -> repository.discoverMoviesByGenre(filterId, page = nextPage)
					JellyseerrDiscoverCategory.TV_GENRE -> repository.discoverTvByGenre(filterId, page = nextPage)
					JellyseerrDiscoverCategory.MOVIE_STUDIOS -> repository.discoverMoviesByStudio(filterId, page = nextPage)
					JellyseerrDiscoverCategory.TV_NETWORKS -> repository.discoverTvByNetwork(filterId, page = nextPage)
					else -> return@launch
				}

				if (discoveryResult.isFailure) {
					val error = discoveryResult.exceptionOrNull()
					state.update {
						it.copy(
							isLoading = false,
							errorMessage = error?.message,
						)
					}
					return@launch
				}

				when (current.discoverCategory) {
					JellyseerrDiscoverCategory.MOVIE_GENRE, JellyseerrDiscoverCategory.TV_GENRE -> {
						val discovery = discoveryResult.getOrThrow() as JellyseerrGenreDiscovery

						if (discovery.results.isEmpty()) {
							state.update {
								it.copy(
									isLoading = false,
									discoverCurrentPage = nextPage,
									discoverHasMore = false,
									discoverGenre = discovery.genre,
									discoverCompany = null,
									discoverTitle = discovery.genre.name,
								)
							}
						} else {
							val resultsWithAvailability = repository.markAvailableInJellyfin(discovery.results)
								.getOrElse { discovery.results }
							val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

							state.update {
								it.copy(
									isLoading = false,
									results = it.results + marked,
									discoverCurrentPage = nextPage,
									discoverHasMore = true,
									discoverGenre = discovery.genre,
									discoverCompany = null,
									discoverTitle = discovery.genre.name,
								)
							}
						}
					}
					JellyseerrDiscoverCategory.MOVIE_STUDIOS, JellyseerrDiscoverCategory.TV_NETWORKS -> {
						val discovery = discoveryResult.getOrThrow() as JellyseerrCompanyDiscovery

						if (discovery.results.isEmpty()) {
							state.update {
								it.copy(
									isLoading = false,
									discoverCurrentPage = nextPage,
									discoverHasMore = false,
									discoverGenre = null,
									discoverCompany = discovery.company,
									discoverTitle = discovery.company.name,
								)
							}
						} else {
							val resultsWithAvailability = repository.markAvailableInJellyfin(discovery.results)
								.getOrElse { discovery.results }
							val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

							state.update {
								it.copy(
									isLoading = false,
									results = it.results + marked,
									discoverCurrentPage = nextPage,
									discoverHasMore = true,
									discoverGenre = null,
									discoverCompany = discovery.company,
									discoverTitle = discovery.company.name,
								)
							}
						}
					}
					else -> {
						state.update { it.copy(isLoading = false) }
					}
				}

				return@launch
			}

			val discoverResult = when (current.discoverCategory) {
				JellyseerrDiscoverCategory.TRENDING -> repository.discoverTrending(page = nextPage)
				JellyseerrDiscoverCategory.POPULAR_MOVIES -> repository.discoverMovies(page = nextPage)
				JellyseerrDiscoverCategory.UPCOMING_MOVIES -> repository.discoverUpcomingMovies(page = nextPage)
				JellyseerrDiscoverCategory.POPULAR_TV -> repository.discoverTv(page = nextPage)
				JellyseerrDiscoverCategory.UPCOMING_TV -> repository.discoverUpcomingTv(page = nextPage)
				else -> return@launch
			}

			if (discoverResult.isFailure) {
				val error = discoverResult.exceptionOrNull()
				state.update {
					it.copy(
						isLoading = false,
						errorMessage = error?.message,
					)
				}
				return@launch
			}

			val results = discoverResult.getOrThrow()

			if (results.isEmpty()) {
				state.update {
					it.copy(
						isLoading = false,
						discoverCurrentPage = nextPage,
						discoverHasMore = false,
					)
				}
			} else {
				val resultsWithAvailability = repository.markAvailableInJellyfin(results).getOrElse { results }
				val marked = JellyseerrRequestMarkers.markItemsWithRequests(resultsWithAvailability, currentRequests)

				state.update {
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

	fun loadMoreSearchResults() {
		val current = state.value
		if (!current.showSearchResultsGrid || current.isLoading || !current.searchHasMore) return
		search(page = current.searchCurrentPage + 1)
	}

	fun closeAllTrends() {
		state.update {
			it.copy(
				showAllTrendsGrid = false,
				showSearchResultsGrid = false,
				discoverTitle = null,
				discoverGenre = null,
				discoverGenreMediaType = null,
				discoverCompany = null,
			)
		}

		scope.launch {
			requestActions.refreshOwnRequests()
			loadRecentRequests()
		}
	}

	suspend fun loadMovieGenres() {
		repository.getMovieGenres()
			.onSuccess { genres ->
				state.update { it.copy(movieGenres = genres) }
			}
			.onFailure { _ ->
			}
	}

	suspend fun loadTvGenres() {
		repository.getTvGenres()
			.onSuccess { genres ->
				state.update { it.copy(tvGenres = genres) }
			}
			.onFailure { _ ->
			}
	}
}
