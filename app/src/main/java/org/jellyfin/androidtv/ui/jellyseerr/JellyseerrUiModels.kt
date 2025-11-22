package org.jellyfin.androidtv.ui.jellyseerr

import org.jellyfin.androidtv.data.repository.JellyseerrCompany
import org.jellyfin.androidtv.data.repository.JellyseerrEpisode
import org.jellyfin.androidtv.data.repository.JellyseerrGenre
import org.jellyfin.androidtv.data.repository.JellyseerrGenreSlider
import org.jellyfin.androidtv.data.repository.JellyseerrMovieDetails
import org.jellyfin.androidtv.data.repository.JellyseerrPersonDetails
import org.jellyfin.androidtv.data.repository.JellyseerrRequest
import org.jellyfin.androidtv.data.repository.JellyseerrSearchItem

enum class JellyseerrDiscoverCategory(val titleResId: Int) {
	TRENDING(org.jellyfin.androidtv.R.string.jellyseerr_discover_title),
	POPULAR_MOVIES(org.jellyfin.androidtv.R.string.jellyseerr_popular_title),
	UPCOMING_MOVIES(org.jellyfin.androidtv.R.string.jellyseerr_upcoming_movies_title),
	POPULAR_TV(org.jellyfin.androidtv.R.string.jellyseerr_popular_tv_title),
	UPCOMING_TV(org.jellyfin.androidtv.R.string.jellyseerr_upcoming_tv_title),
	MOVIE_GENRE(org.jellyfin.androidtv.R.string.jellyseerr_movie_genres_title),
	TV_GENRE(org.jellyfin.androidtv.R.string.jellyseerr_tv_genres_title),
	MOVIE_STUDIOS(org.jellyfin.androidtv.R.string.jellyseerr_movie_studios_title),
	TV_NETWORKS(org.jellyfin.androidtv.R.string.jellyseerr_tv_networks_title),
}

data class SeasonKey(val tmdbId: Int, val seasonNumber: Int)

data class JellyseerrUiState(
	val isLoading: Boolean = false,
	val query: String = "",
	val results: List<JellyseerrSearchItem> = emptyList(),
	val ownRequests: List<JellyseerrRequest> = emptyList(),
	val errorMessage: String? = null,
	val selectedItem: JellyseerrSearchItem? = null,
	val selectedMovie: JellyseerrMovieDetails? = null,
	val showAllTrendsGrid: Boolean = false,
	val showSearchResultsGrid: Boolean = false,
	val requestStatusMessage: String? = null,
	val discoverCurrentPage: Int = 1,
	val discoverHasMore: Boolean = true,
	val selectedPerson: JellyseerrPersonDetails? = null,
	val personCredits: List<JellyseerrSearchItem> = emptyList(),
	val originDetailItem: JellyseerrSearchItem? = null,
	val trendingResults: List<JellyseerrSearchItem> = emptyList(),
	val popularResults: List<JellyseerrSearchItem> = emptyList(),
	val recentRequests: List<JellyseerrSearchItem> = emptyList(),
	val popularTvResults: List<JellyseerrSearchItem> = emptyList(),
	val upcomingMovieResults: List<JellyseerrSearchItem> = emptyList(),
	val upcomingTvResults: List<JellyseerrSearchItem> = emptyList(),
	val discoverCategory: JellyseerrDiscoverCategory = JellyseerrDiscoverCategory.TRENDING,
	val discoverTitle: String? = null,
	val discoverGenre: JellyseerrGenre? = null,
	val discoverCompany: JellyseerrCompany? = null,
	val discoverGenreMediaType: String? = null,
	val scrollPositions: Map<String, ScrollPosition> = emptyMap(),
	val lastFocusedItemId: Int? = null,
	val lastFocusedViewAllKey: String? = null,
	val seasonEpisodes: Map<SeasonKey, List<JellyseerrEpisode>> = emptyMap(),
	val loadingSeasonKeys: Set<SeasonKey> = emptySet(),
	val seasonErrors: Map<SeasonKey, String> = emptyMap(),
	val movieGenres: List<JellyseerrGenreSlider> = emptyList(),
	val tvGenres: List<JellyseerrGenreSlider> = emptyList(),
	val searchCurrentPage: Int = 0,
	val searchTotalPages: Int = 0,
	val searchHasMore: Boolean = false,
)

data class ScrollPosition(
	val index: Int = 0,
	val offset: Int = 0,
)
