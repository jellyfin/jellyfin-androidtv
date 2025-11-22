package org.jellyfin.androidtv.data.repository

interface JellyseerrRepository {
	suspend fun ensureConfig(): Result<Unit>
	suspend fun search(query: String, page: Int = 1): Result<JellyseerrSearchResult>
	suspend fun getOwnRequests(): Result<List<JellyseerrRequest>>
	suspend fun getRecentRequests(): Result<List<JellyseerrSearchItem>>
	suspend fun createRequest(item: JellyseerrSearchItem, seasons: List<Int>? = null): Result<Unit>
	suspend fun discoverTrending(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun discoverMovies(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun discoverTv(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun discoverUpcomingMovies(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun discoverUpcomingTv(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun discoverMoviesByGenre(genreId: Int, page: Int = 1): Result<JellyseerrGenreDiscovery>
	suspend fun discoverTvByGenre(genreId: Int, page: Int = 1): Result<JellyseerrGenreDiscovery>
	suspend fun discoverMoviesByStudio(studioId: Int, page: Int = 1): Result<JellyseerrCompanyDiscovery>
	suspend fun discoverTvByNetwork(networkId: Int, page: Int = 1): Result<JellyseerrCompanyDiscovery>
	suspend fun getMovieDetails(tmdbId: Int): Result<JellyseerrMovieDetails>
	suspend fun getTvDetails(tmdbId: Int): Result<JellyseerrMovieDetails>
	suspend fun getSeasonEpisodes(tmdbId: Int, seasonNumber: Int): Result<List<JellyseerrEpisode>>
	suspend fun cancelRequest(requestId: Int): Result<Unit>
	suspend fun markAvailableInJellyfin(items: List<JellyseerrSearchItem>): Result<List<JellyseerrSearchItem>>
	suspend fun getPersonDetails(personId: Int): Result<JellyseerrPersonDetails>
	suspend fun getPersonCredits(personId: Int): Result<List<JellyseerrSearchItem>>
	suspend fun getMovieGenres(): Result<List<JellyseerrGenreSlider>>
	suspend fun getTvGenres(): Result<List<JellyseerrGenreSlider>>
}
