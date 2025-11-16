package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber

private const val TMDB_POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500"
private const val TMDB_BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w780"
private const val TMDB_PROFILE_BASE_URL = "https://image.tmdb.org/t/p/w300"

private fun posterImageUrl(path: String?): String? = path?.takeIf { it.isNotBlank() }?.let { "$TMDB_POSTER_BASE_URL$it" }
private fun backdropImageUrl(path: String?): String? = path?.takeIf { it.isNotBlank() }?.let { "$TMDB_BACKDROP_BASE_URL$it" }
private fun profileImageUrl(path: String?): String? = path?.takeIf { it.isNotBlank() }?.let { "$TMDB_PROFILE_BASE_URL$it" }

interface JellyseerrRepository {
	suspend fun search(query: String): Result<List<JellyseerrSearchItem>>
	suspend fun getOwnRequests(): Result<List<JellyseerrRequest>>
	suspend fun getRecentRequests(): Result<List<JellyseerrSearchItem>>
	suspend fun createRequest(item: JellyseerrSearchItem, seasons: List<Int>? = null): Result<Unit>
	suspend fun discoverTrending(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun discoverMovies(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun discoverTv(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun discoverUpcomingMovies(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun discoverUpcomingTv(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun getMovieDetails(tmdbId: Int): Result<JellyseerrMovieDetails>
	suspend fun getTvDetails(tmdbId: Int): Result<JellyseerrMovieDetails>
	suspend fun getSeasonEpisodes(tmdbId: Int, seasonNumber: Int): Result<List<JellyseerrEpisode>>
	suspend fun cancelRequest(requestId: Int): Result<Unit>
	suspend fun markAvailableInJellyfin(items: List<JellyseerrSearchItem>): Result<List<JellyseerrSearchItem>>
	suspend fun getPersonDetails(personId: Int): Result<JellyseerrPersonDetails>
    suspend fun getPersonCredits(personId: Int): Result<List<JellyseerrSearchItem>>
}

data class JellyseerrSearchItem(
	val id: Int,
	val mediaType: String,
	val title: String,
	val overview: String?,
	val posterPath: String? = null,
	val backdropPath: String? = null,
	val releaseDate: String? = null,
	val isRequested: Boolean = false,
	val isAvailable: Boolean = false,
	val isPartiallyAvailable: Boolean = false,
	val requestId: Int? = null,
	val requestStatus: Int? = null,
	val jellyfinId: String? = null,
)

@Serializable
private data class JellyseerrSearchResponse(
	val page: Int,
	val totalPages: Int,
	val totalResults: Int,
	val results: List<JellyseerrSearchItemDto>,
)

@Serializable
private data class JellyseerrSearchItemDto(
	val id: Int,
	val mediaType: String,
	val title: String? = null,
	val name: String? = null,
	val overview: String? = null,
	val posterPath: String? = null,
	val backdropPath: String? = null,
	val mediaInfo: JellyseerrMediaInfoDto? = null,
)

@Serializable
private data class JellyseerrMediaInfoDto(
	val id: Int? = null,
	val status: Int? = null,
	val status4k: Int? = null,
	val jellyfinMediaId: String? = null,
)

@Serializable
private data class JellyseerrUserResults(
	val results: List<JellyseerrUser>,
)

@Serializable
private data class JellyseerrUser(
	val id: Int,
	val username: String? = null,
	val jellyfinUsername: String? = null,
	val jellyfinUserId: String? = null,
)

@Serializable
data class JellyseerrRequestPage(
	val results: List<JellyseerrRequestDto>,
)

@Serializable
data class JellyseerrRequestDto(
	val id: Int,
	val status: Int? = null,
	val type: String? = null,
	val media: JellyseerrMediaDto? = null,
)

@Serializable
data class JellyseerrMediaDto(
	val tmdbId: Int? = null,
	val mediaType: String? = null,
	val title: String? = null,
	val name: String? = null,
	val posterPath: String? = null,
	val backdropPath: String? = null,
)

@Serializable
data class JellyseerrPersonDetails(
    val id: Int,
    val name: String,
    val birthday: String? = null,
    val deathday: String? = null,
    val knownForDepartment: String? = null,
    val alsoKnownAs: List<String> = emptyList(),
    val gender: Int? = null,
    val biography: String? = null,
    val popularity: Double? = null,
    val placeOfBirth: String? = null,
    val profilePath: String? = null,
    val adult: Boolean? = null,
    val imdbId: String? = null,
    val homepage: String? = null,
)

@Serializable
private data class JellyseerrPersonCredit(
    val id: Int,
    val mediaType: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val title: String? = null,
    val name: String? = null,
    val adult: Boolean? = null,
)

@Serializable
private data class JellyseerrCombinedCredits(
    val id: Int,
    val cast: List<JellyseerrPersonCredit> = emptyList(),
    val crew: List<JellyseerrPersonCredit> = emptyList(),
)

data class JellyseerrRequest(
	val id: Int,
	val status: Int?,
	val mediaType: String?,
	val title: String,
	val tmdbId: Int?,
	val posterPath: String? = null,
	val backdropPath: String? = null,
)

@Serializable
private data class JellyseerrCreateRequestBody(
	val mediaType: String,
	@SerialName("mediaId")
	val mediaId: Int,
	val userId: Int,
	val seasons: List<Int>? = null,
)

@Serializable
data class JellyseerrGenre(
	val id: Int,
	val name: String,
)

@Serializable
data class JellyseerrCast(
	val id: Int,
	val castId: Int? = null,
	val character: String? = null,
	val creditId: String? = null,
	val gender: Int? = null,
	val name: String,
	val order: Int? = null,
	val profilePath: String? = null,
)

@Serializable
data class JellyseerrCredits(
	val cast: List<JellyseerrCast> = emptyList(),
)

@Serializable
data class JellyseerrSeason(
	val id: Int,
	val name: String? = null,
	val overview: String? = null,
	val posterPath: String? = null,
	val seasonNumber: Int,
	val episodeCount: Int? = null,
	val airDate: String? = null,
	val status: Int? = null,
)

data class JellyseerrEpisode(
	val id: Int,
	val name: String? = null,
	val overview: String? = null,
	val imageUrl: String? = null,
	val episodeNumber: Int? = null,
	val seasonNumber: Int? = null,
	val airDate: String? = null,
	val isMissing: Boolean = false,
	val isAvailable: Boolean = false,
	val jellyfinId: String? = null,
)

@Serializable
data class JellyseerrMovieDetails(
	val id: Int,
	val title: String? = null,
	val name: String? = null,
	val originalTitle: String? = null,
	val overview: String? = null,
	val posterPath: String? = null,
	val backdropPath: String? = null,
	val releaseDate: String? = null,
	val firstAirDate: String? = null,
	val runtime: Int? = null,
	val voteAverage: Double? = null,
	val genres: List<JellyseerrGenre> = emptyList(),
	val credits: JellyseerrCredits? = null,
	val seasons: List<JellyseerrSeason> = emptyList(),
)

class JellyseerrRepositoryImpl(
	private val userPreferences: UserPreferences,
	private val sessionRepository: SessionRepository,
	private val userRepository: UserRepository,
	private val apiClient: ApiClient,
) : JellyseerrRepository {
	private data class TvAvailability(
		val isFullyAvailable: Boolean,
		val isPartiallyAvailable: Boolean,
	)

	private val tvAvailabilityCache = mutableMapOf<Int, TvAvailability>()

	private fun clearTvAvailabilityCache() {
		tvAvailabilityCache.clear()
	}

	private suspend fun lookupTvAvailability(tmdbId: Int): TvAvailability? {
		tvAvailabilityCache[tmdbId]?.let { return it }
		val result = getTvDetails(tmdbId)
		if (result.isFailure) return null

		val details = result.getOrNull() ?: return null
		val seasons = details.seasons.filter { it.seasonNumber > 0 }
		if (seasons.isEmpty()) return null

		val totalSeasons = seasons.size
		val availableSeasons = seasons.count { it.status == 5 }
		if (availableSeasons == 0) return null

		val availability = TvAvailability(
			isFullyAvailable = availableSeasons == totalSeasons,
			isPartiallyAvailable = availableSeasons < totalSeasons,
		)
		tvAvailabilityCache[tmdbId] = availability
		return availability
	}

	private data class Config(val baseUrl: String, val apiKey: String)

	private val client = OkHttpClient()
	private val json = Json {
		ignoreUnknownKeys = true
	}

	private fun mapSearchItemDtoToModel(dto: JellyseerrSearchItemDto): JellyseerrSearchItem {
		val posterUrl = posterImageUrl(dto.posterPath)
		val backdropUrl = backdropImageUrl(dto.backdropPath)

		// Status aus mediaInfo extrahieren (von Jellyseerr)
		// Status 5 = Available, Status 4 = Partially Available
		val status = dto.mediaInfo?.status
		val isAvailable = status == 5
		val isPartiallyAvailable = status == 4
		val jellyfinId = dto.mediaInfo?.jellyfinMediaId

		return JellyseerrSearchItem(
			id = dto.id,
			mediaType = dto.mediaType,
			title = (dto.title ?: dto.name).orEmpty(),
			overview = dto.overview,
			posterPath = posterUrl,
			backdropPath = backdropUrl,
			isAvailable = isAvailable,
			isPartiallyAvailable = isPartiallyAvailable,
			jellyfinId = jellyfinId,
		)
	}

	// Cached mapping between current Jellyfin user and Jellyseerr user id
	@Volatile
	private var cachedUserId: Int? = null

	private fun getConfig(): Config? {
		val url = userPreferences[UserPreferences.jellyseerrUrl].trim()
		val apiKey = userPreferences[UserPreferences.jellyseerrApiKey].trim()

		if (url.isBlank() || apiKey.isBlank()) return null

		val baseUrl = url.trimEnd('/')
		return Config(baseUrl, apiKey)
	}

	private suspend fun resolveCurrentUserId(config: Config): Result<Int> = withContext(Dispatchers.IO) {
		cachedUserId?.let { return@withContext Result.success(it) }

		val session = sessionRepository.currentSession.value
			?: return@withContext Result.failure(IllegalStateException("No active Jellyfin session"))
		val jellyfinUserId = session.userId.toString()
		val jellyfinUsername = userRepository.currentUser.value?.name.orEmpty()

		// Jellyseerr supports a search query (q) over username/email/plex/jellyfin usernames.
		val encodedName = if (jellyfinUsername.isNotBlank()) {
			java.net.URLEncoder.encode(jellyfinUsername, Charsets.UTF_8.name())
		} else null

		val url = buildString {
			append(config.baseUrl)
			append("/api/v1/user?take=50")
			if (!encodedName.isNullOrBlank()) {
				append("&q=")
				append(encodedName)
			}
		}
		val request = Request.Builder()
			.url(url)
			.header("X-API-Key", config.apiKey)
			.build()

		runCatching {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
				val body = response.body?.string() ?: throw IllegalStateException("Empty body")
				val result = json.decodeFromString(JellyseerrUserResults.serializer(), body)

				// Prefer exact match on Jellyfin user id
				val byId = result.results.firstOrNull {
					!it.jellyfinUserId.isNullOrBlank() &&
						it.jellyfinUserId.equals(jellyfinUserId, ignoreCase = true)
				}

				// Fallback: match on Jellyfin username / username
				val byName = if (!jellyfinUsername.isBlank()) {
					result.results.firstOrNull {
						it.jellyfinUsername?.equals(jellyfinUsername, ignoreCase = true) == true ||
							it.username?.equals(jellyfinUsername, ignoreCase = true) == true
					}
				} else null

				val user = byId ?: byName
					?: throw IllegalStateException("No Jellyseerr user linked to current Jellyfin user")

				cachedUserId = user.id
				user.id
			}
		}
	}

	override suspend fun markAvailableInJellyfin(items: List<JellyseerrSearchItem>): Result<List<JellyseerrSearchItem>> =
		withContext(Dispatchers.IO) {
			if (items.isEmpty()) return@withContext Result.success(emptyList())

			runCatching {
				items.map { item ->
					// Wenn bereits eine JellyfinId von Jellyseerr vorhanden ist, behalte alle Daten
					if (item.jellyfinId != null) return@map item

					val title = item.title.trim()
					if (title.isEmpty()) return@map item

					// Nur für verfügbare Items suchen wir die Jellyfin-ID für Navigation
					if (!item.isAvailable && !item.isPartiallyAvailable) return@map item

					// Bestimme den Item-Typ basierend auf mediaType
					val itemKind = when (item.mediaType) {
						"movie" -> BaseItemKind.MOVIE
						"tv" -> BaseItemKind.SERIES
						else -> return@map item
					}

					val request = GetItemsRequest(
						searchTerm = title,
						limit = 5,
						includeItemTypes = setOf(itemKind),
						recursive = true,
						enableTotalRecordCount = false,
					)

					val resultItems = try {
						apiClient.itemsApi.getItems(request).content.items
					} catch (error: ApiClientException) {
						Timber.w(error, "Failed to query Jellyfin for Jellyfin ID of \"%s\"", title)
						return@map item
					}

					// Finde exakten Match
					val matchedItem = resultItems?.firstOrNull { baseItem ->
						val name = baseItem.name?.trim().orEmpty()
						val original = baseItem.originalTitle?.trim().orEmpty()
						name.equals(title, ignoreCase = true) ||
							original.equals(title, ignoreCase = true)
					}

					if (matchedItem != null) {
						// Nur die Jellyfin-ID für Navigation ergänzen
						// Verfügbarkeitsinformationen bleiben aus Jellyseerr
						item.copy(
							jellyfinId = matchedItem.id.toString(),
						)
					} else {
						item
					}
				}
			}.onFailure {
				Timber.e(it, "Failed to lookup Jellyfin IDs for Jellyseerr items")
			}
		}

	override suspend fun search(query: String): Result<List<JellyseerrSearchItem>> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		if (query.isBlank()) return@withContext Result.success(emptyList())

		val encodedQuery = java.net.URLEncoder.encode(query, Charsets.UTF_8.name())
		val url = "${config.baseUrl}/api/v1/search?query=$encodedQuery&page=1"

		val request = Request.Builder()
			.url(url)
			.header("X-API-Key", config.apiKey)
			.build()

		runCatching {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")

				val body = response.body?.string() ?: throw IllegalStateException("Empty body")
				val result = json.decodeFromString(JellyseerrSearchResponse.serializer(), body)

				result.results
					.filter { it.mediaType == "movie" || it.mediaType == "tv" }
					.map { mapSearchItemDtoToModel(it) }
			}
		}.onFailure {
			Timber.e(it, "Failed to search Jellyseerr")
		}
	}

	override suspend fun getOwnRequests(): Result<List<JellyseerrRequest>> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }
		val url = "${config.baseUrl}/api/v1/request?take=50&requestedBy=$userId&sort=modified&sortDirection=desc"
		val request = Request.Builder()
			.url(url)
			.header("X-API-Key", config.apiKey)
			.header("X-API-User", userId.toString())
			.build()

		runCatching {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")

				val body = response.body?.string() ?: throw IllegalStateException("Empty body")
				val page = json.decodeFromString(JellyseerrRequestPage.serializer(), body)

				page.results.map { dto ->
					val title = dto.media?.title ?: dto.media?.name ?: ""
					val posterUrl = posterImageUrl(dto.media?.posterPath)
					val backdropUrl = backdropImageUrl(dto.media?.backdropPath)
					JellyseerrRequest(
						id = dto.id,
						status = dto.status,
						mediaType = dto.media?.mediaType,
						title = title,
						tmdbId = dto.media?.tmdbId,
						posterPath = posterUrl,
						backdropPath = backdropUrl,
					)
				}
			}
		}.onFailure {
			Timber.e(it, "Failed to load Jellyseerr requests")
		}
	}

	override suspend fun getRecentRequests(): Result<List<JellyseerrSearchItem>> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }
		// Alle Anfragen vom aktuellen User anzeigen - verwende requestedBy Parameter, take=100 für alle
		val url = "${config.baseUrl}/api/v1/request?requestedBy=${userId}&take=100&sort=modified&skip=0"
		val request = Request.Builder()
			.url(url)
			.header("X-API-Key", config.apiKey)
			.header("X-API-User", userId.toString())
			.build()

		runCatching {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")

				val body = response.body?.string() ?: throw IllegalStateException("Empty body")
				val page = json.decodeFromString(JellyseerrRequestPage.serializer(), body)

				Timber.d("Jellyseerr recent requests: found ${page.results.size} requests")

				// Für jeden Request holen wir die vollständigen Details von Jellyseerr, um Poster zu bekommen
				page.results.mapNotNull { dto ->
					val tmdbId = dto.media?.tmdbId ?: return@mapNotNull null
					val mediaType = dto.media?.mediaType ?: return@mapNotNull null

					// Hole die vollständigen Details von Jellyseerr (enthält Poster-Daten)
					val detailsUrl = if (mediaType == "movie") {
						"${config.baseUrl}/api/v1/movie/$tmdbId"
					} else {
						"${config.baseUrl}/api/v1/tv/$tmdbId"
					}

					val detailsRequest = Request.Builder()
						.url(detailsUrl)
						.header("X-API-Key", config.apiKey)
						.header("X-API-User", userId.toString())
						.build()

					try {
						client.newCall(detailsRequest).execute().use { detailsResponse ->
							if (!detailsResponse.isSuccessful) {
								Timber.w("Failed to load details for $mediaType $tmdbId: HTTP ${detailsResponse.code}")
								return@mapNotNull null
							}

							val detailsBody = detailsResponse.body?.string() ?: return@mapNotNull null
							val details = json.decodeFromString(JellyseerrMovieDetails.serializer(), detailsBody)

							val posterUrl = posterImageUrl(details.posterPath)
							val backdropUrl = backdropImageUrl(details.backdropPath)
							val dateValue = details.releaseDate ?: details.firstAirDate
							val titleValue = details.title ?: details.name ?: ""

							JellyseerrSearchItem(
								id = tmdbId,
								mediaType = mediaType,
								title = titleValue,
								overview = details.overview,
								posterPath = posterUrl,
								backdropPath = backdropUrl,
								releaseDate = dateValue,
								isRequested = dto.status != null && dto.status != 5,
								isAvailable = dto.status == 5,
								isPartiallyAvailable = false,
								requestId = dto.id,
								requestStatus = dto.status,
							)
						}
					} catch (e: Exception) {
						Timber.e(e, "Failed to load details for $mediaType $tmdbId")
						null
					}
				}
			}
		}.onFailure {
			Timber.e(it, "Failed to load Jellyseerr recent requests")
		}
	}

	override suspend fun createRequest(item: JellyseerrSearchItem, seasons: List<Int>?): Result<Unit> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }

		val body = JellyseerrCreateRequestBody(
			mediaType = item.mediaType,
			mediaId = item.id,
			userId = userId,
			seasons = seasons,
		)

		val jsonBody = json.encodeToString(JellyseerrCreateRequestBody.serializer(), body)
		val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

		val url = "${config.baseUrl}/api/v1/request"
		val request = Request.Builder()
			.url(url)
			.header("X-API-Key", config.apiKey)
			.header("X-API-User", userId.toString())
			.post(requestBody)
			.build()

		runCatching {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
			}
		}.onSuccess {
			clearTvAvailabilityCache()
		}.onFailure {
			Timber.e(it, "Failed to create Jellyseerr request")
		}
	}

	override suspend fun cancelRequest(requestId: Int): Result<Unit> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }

		val url = "${config.baseUrl}/api/v1/request/$requestId"
		val request = Request.Builder()
			.url(url)
			.header("X-API-Key", config.apiKey)
			.header("X-API-User", userId.toString())
			.delete()
			.build()

		runCatching {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
			}
		}.onSuccess {
			clearTvAvailabilityCache()
		}.onFailure {
			Timber.e(it, "Failed to cancel Jellyseerr request")
		}
	}

	override suspend fun discoverTrending(page: Int): Result<List<JellyseerrSearchItem>> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }

		val url = "${config.baseUrl}/api/v1/discover/trending?page=$page"
		val request = Request.Builder()
			.url(url)
			.header("X-API-Key", config.apiKey)
			.header("X-API-User", userId.toString())
			.build()

		runCatching {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")

				val body = response.body?.string() ?: throw IllegalStateException("Empty body")
				val result = json.decodeFromString(JellyseerrSearchResponse.serializer(), body)

				result.results
					.filter { it.mediaType == "movie" || it.mediaType == "tv" }
					.map { mapSearchItemDtoToModel(it) }
			}
		}.onFailure {
			Timber.e(it, "Failed to load Jellyseerr trending titles")
		}
	}

	override suspend fun discoverMovies(page: Int): Result<List<JellyseerrSearchItem>> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }

		val url = "${config.baseUrl}/api/v1/discover/movies?page=$page"
		val request = Request.Builder()
			.url(url)
			.header("X-API-Key", config.apiKey)
			.header("X-API-User", userId.toString())
			.build()

		runCatching {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")

				val body = response.body?.string() ?: throw IllegalStateException("Empty body")
				val result = json.decodeFromString(JellyseerrSearchResponse.serializer(), body)

				result.results
					.filter { it.mediaType == "movie" || it.mediaType == "tv" }
					.map { mapSearchItemDtoToModel(it) }
			}
		}.onFailure {
			Timber.e(it, "Failed to load Jellyseerr discover movies")
		}
	}

	override suspend fun discoverTv(page: Int): Result<List<JellyseerrSearchItem>> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }

		val url = "${config.baseUrl}/api/v1/discover/tv?page=$page"
		val request = Request.Builder()
			.url(url)
			.header("X-API-Key", config.apiKey)
			.header("X-API-User", userId.toString())
			.build()

		runCatching {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")

				val body = response.body?.string() ?: throw IllegalStateException("Empty body")
				val result = json.decodeFromString(JellyseerrSearchResponse.serializer(), body)

				result.results
					.filter { it.mediaType == "movie" || it.mediaType == "tv" }
					.map { mapSearchItemDtoToModel(it) }
			}
		}.onFailure {
			Timber.e(it, "Failed to load Jellyseerr discover tv")
		}
	}

	override suspend fun discoverUpcomingMovies(page: Int): Result<List<JellyseerrSearchItem>> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }

		val url = "${config.baseUrl}/api/v1/discover/movies/upcoming?page=$page"
		val request = Request.Builder()
			.url(url)
			.header("X-API-Key", config.apiKey)
			.header("X-API-User", userId.toString())
			.build()

		runCatching {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")

				val body = response.body?.string() ?: throw IllegalStateException("Empty body")
				val result = json.decodeFromString(JellyseerrSearchResponse.serializer(), body)

				result.results
					.filter { it.mediaType == "movie" || it.mediaType == "tv" }
					.map { mapSearchItemDtoToModel(it) }
			}
		}.onFailure {
			Timber.e(it, "Failed to load Jellyseerr upcoming movies")
		}
	}

	override suspend fun discoverUpcomingTv(page: Int): Result<List<JellyseerrSearchItem>> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }

		val url = "${config.baseUrl}/api/v1/discover/tv/upcoming?page=$page"
		val request = Request.Builder()
			.url(url)
			.header("X-API-Key", config.apiKey)
			.header("X-API-User", userId.toString())
			.build()

		runCatching {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")

				val body = response.body?.string() ?: throw IllegalStateException("Empty body")
				val result = json.decodeFromString(JellyseerrSearchResponse.serializer(), body)

				result.results
					.filter { it.mediaType == "movie" || it.mediaType == "tv" }
					.map { mapSearchItemDtoToModel(it) }
			}
		}.onFailure {
			Timber.e(it, "Failed to load Jellyseerr upcoming tv")
		}
	}

@Serializable
private data class MediaInfo(
	val seasons: List<MediaInfoSeason> = emptyList(),
	val requests: List<MediaInfoRequest> = emptyList(),
)

	@Serializable
	private data class MediaInfoSeason(
		val seasonNumber: Int,
		val status: Int? = null,
		val episodes: List<MediaInfoEpisode> = emptyList(),
	)

	@Serializable
	private data class MediaInfoEpisode(
		val id: Int,
		val episodeNumber: Int,
		val status: Int? = null,
	)

	@Serializable
	private data class MediaInfoRequest(
		val id: Int,
		val status: Int? = null,
		val is4k: Boolean = false,
		val seasons: List<MediaInfoRequestSeason> = emptyList(),
	)

	@Serializable
	private data class MediaInfoRequestSeason(
		val id: Int,
		val seasonNumber: Int,
		val status: Int? = null,
	)

@Serializable
private data class JellyseerrTvDetailsRaw(
	val id: Int,
	val name: String? = null,
	val overview: String? = null,
	val posterPath: String? = null,
	val backdropPath: String? = null,
	val firstAirDate: String? = null,
	val episodeRunTime: List<Int> = emptyList(),
	val voteAverage: Double? = null,
	val genres: List<JellyseerrGenre> = emptyList(),
	val credits: JellyseerrCredits? = null,
	val seasons: List<JellyseerrSeason> = emptyList(),
	val mediaInfo: MediaInfo? = null,
)

@Serializable
private data class JellyseerrSeasonDetailsRaw(
	val id: Int,
	val name: String? = null,
	val overview: String? = null,
	val posterPath: String? = null,
	val seasonNumber: Int,
	val episodeCount: Int? = null,
	val airDate: String? = null,
	val status: Int? = null,
	val episodes: List<JellyseerrEpisodeRaw> = emptyList(),
)

@Serializable
private data class JellyseerrEpisodeRaw(
	val id: Int,
	val name: String? = null,
	val overview: String? = null,
	val stillPath: String? = null,
	val posterPath: String? = null,
	val episodeNumber: Int? = null,
	val seasonNumber: Int? = null,
	val airDate: String? = null,
	val missing: Boolean = false,
)

	override suspend fun getMovieDetails(tmdbId: Int): Result<JellyseerrMovieDetails> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }

		val url = "${config.baseUrl}/api/v1/movie/$tmdbId"
		val request = Request.Builder()
			.url(url)
			.header("X-API-Key", config.apiKey)
			.header("X-API-User", userId.toString())
			.build()

		runCatching {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")

				val body = response.body?.string() ?: throw IllegalStateException("Empty body")
				val raw = json.decodeFromString(JellyseerrMovieDetails.serializer(), body)

				val mappedCredits = raw.credits?.let { credits ->
					val mappedCast = credits.cast.map { castMember ->
						val profile = profileImageUrl(castMember.profilePath)
						castMember.copy(profilePath = profile)
					}
					credits.copy(cast = mappedCast)
				}

				raw.copy(
					posterPath = posterImageUrl(raw.posterPath),
					backdropPath = backdropImageUrl(raw.backdropPath),
					credits = mappedCredits ?: raw.credits,
				)
			}
		}.onFailure {
			Timber.e(it, "Failed to load Jellyseerr movie details")
		}
	}

	override suspend fun getTvDetails(tmdbId: Int): Result<JellyseerrMovieDetails> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }

		val url = "${config.baseUrl}/api/v1/tv/$tmdbId"
		val request = Request.Builder()
			.url(url)
			.header("X-API-Key", config.apiKey)
			.header("X-API-User", userId.toString())
			.build()

		runCatching {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")

				val body = response.body?.string() ?: throw IllegalStateException("Empty body")
				val raw = json.decodeFromString(JellyseerrTvDetailsRaw.serializer(), body)

				val mappedCredits = raw.credits?.let { credits ->
					val mappedCast = credits.cast.map { castMember ->
						val profile = profileImageUrl(castMember.profilePath)
						castMember.copy(profilePath = profile)
					}
					credits.copy(cast = mappedCast)
				}

				// Merge status from mediaInfo into seasons
				// Wie Jellyseerr: Extrahiere angefragte Seasons aus requests
				val requestedSeasonNumbers = (raw.mediaInfo?.requests ?: emptyList())
					.filter { request ->
						request.is4k == false &&
						request.status != null &&
						request.status != 3 // 3 = DECLINED
					}
					.flatMap { request -> request.seasons.map { it.seasonNumber } }
					.toSet()

				// Status map für verfügbare Seasons (mit Episode-Details)
				val mediaInfoSeasons = raw.mediaInfo?.seasons ?: emptyList()
				val availableStatusMap = mediaInfoSeasons.associate { it.seasonNumber to it }

				val mappedSeasons = raw.seasons.map { season ->
					val mediaInfoSeason = availableStatusMap[season.seasonNumber]
					val availabilityStatus = mediaInfoSeason?.status
					val isRequested = requestedSeasonNumbers.contains(season.seasonNumber)

					// Prüfe Episode-Level-Verfügbarkeit
					val episodes = mediaInfoSeason?.episodes ?: emptyList()
					val availableEpisodes = episodes.count { it.status == 5 }
					val totalEpisodes = season.episodeCount ?: episodes.size
					val hasPartialEpisodes = availableEpisodes > 0 && availableEpisodes < totalEpisodes

					// Status-Logik wie in Jellyseerr:
					// - Wenn alle Episoden verfügbar (5), verwende 5
					// - Wenn teilweise verfügbar (mindestens eine Episode), verwende 4
					// - Wenn angefragt und nicht verfügbar, verwende Request-Status (z.B. 2 = Approved)
					// - Sonst null (nicht angefragt, nicht verfügbar)
					val finalStatus = when {
						availabilityStatus == 5 -> 5 // Fully Available
						hasPartialEpisodes -> 4 // Partially Available (some episodes available)
						isRequested -> 2 // Requested/Approved (wird als "angefragt" angezeigt)
						else -> null // Nicht angefragt - ignoriere den alten mediaInfo.seasons Status
					}

					season.copy(
						posterPath = posterImageUrl(season.posterPath),
						status = finalStatus,
					)
				}

				JellyseerrMovieDetails(
					id = raw.id,
					title = raw.name,
					originalTitle = null,
					overview = raw.overview,
					posterPath = posterImageUrl(raw.posterPath),
					backdropPath = backdropImageUrl(raw.backdropPath),
					releaseDate = raw.firstAirDate,
					runtime = raw.episodeRunTime.firstOrNull(),
					voteAverage = raw.voteAverage,
					genres = raw.genres,
					credits = mappedCredits ?: raw.credits,
					seasons = mappedSeasons,
				)
			}
		}.onFailure {
			Timber.e(it, "Failed to load Jellyseerr tv details")
		}
	}

	override suspend fun getSeasonEpisodes(tmdbId: Int, seasonNumber: Int): Result<List<JellyseerrEpisode>> =
 		withContext(Dispatchers.IO) {
 			val config = getConfig()
 				?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

 			val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }

 			// Erst TV-Details laden um den Seriennamen zu bekommen
 			val tvDetailsUrl = "${config.baseUrl}/api/v1/tv/$tmdbId"
 			val tvDetailsRequest = Request.Builder()
 				.url(tvDetailsUrl)
 				.header("X-API-Key", config.apiKey)
 				.header("X-API-User", userId.toString())
 				.build()

 			var seriesName: String? = null
 			try {
 				client.newCall(tvDetailsRequest).execute().use { response ->
 					if (response.isSuccessful) {
 						val body = response.body?.string()
 						if (body != null) {
 							val tvDetails = json.decodeFromString(JellyseerrTvDetailsRaw.serializer(), body)
 							seriesName = tvDetails.name
 						}
 					}
 				}
 			} catch (e: Exception) {
 				Timber.w(e, "Failed to load TV details for series name")
 			}

 			// Prüfe Episode-Verfügbarkeit direkt in Jellyfin
 			val availableEpisodeMap = mutableMapOf<Int, String>() // episodeNumber -> jellyfinId
 			if (!seriesName.isNullOrBlank()) {
 				try {
 					// Suche Serie in Jellyfin
 					val seriesRequest = GetItemsRequest(
 						searchTerm = seriesName,
 						limit = 5,
 						includeItemTypes = setOf(BaseItemKind.SERIES),
 						recursive = true,
 						enableTotalRecordCount = false,
 					)
 					val seriesItems = apiClient.itemsApi.getItems(seriesRequest).content.items

 					val matchedSeries = seriesItems?.firstOrNull { baseItem ->
 						val name = baseItem.name?.trim().orEmpty()
 						name.equals(seriesName, ignoreCase = true)
 					}

 					if (matchedSeries != null) {
 						// Lade Episoden für diese Staffel aus Jellyfin
 						val episodesRequest = GetItemsRequest(
 							parentId = matchedSeries.id,
 							includeItemTypes = setOf(BaseItemKind.EPISODE),
 							recursive = true,
 							enableTotalRecordCount = false,
 						)
 						val jellyfinEpisodes = apiClient.itemsApi.getItems(episodesRequest).content.items

 						jellyfinEpisodes?.forEach { ep ->
 							if (ep.parentIndexNumber == seasonNumber && ep.indexNumber != null) {
 								availableEpisodeMap[ep.indexNumber!!] = ep.id.toString()
 							}
 						}
 					}
 				} catch (e: Exception) {
 					Timber.w(e, "Failed to check episode availability in Jellyfin")
 				}
 			}

 			val url = "${config.baseUrl}/api/v1/tv/$tmdbId/season/$seasonNumber"
 			val request = Request.Builder()
 				.url(url)
 				.header("X-API-Key", config.apiKey)
 				.header("X-API-User", userId.toString())
 				.build()

 			runCatching {
 				client.newCall(request).execute().use { response ->
 					if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")

 					val body = response.body?.string() ?: throw IllegalStateException("Empty body")
 					val raw = json.decodeFromString(JellyseerrSeasonDetailsRaw.serializer(), body)

 					raw.episodes.map { episode ->
 						val imageUrl = episode.stillPath?.let(::posterImageUrl)
 							?: episode.posterPath?.let(::posterImageUrl)

 						val epNumber = episode.episodeNumber ?: 0
 						val jellyfinEpisodeId = availableEpisodeMap[epNumber]
 						val isEpisodeAvailable = jellyfinEpisodeId != null

 						JellyseerrEpisode(
 							id = episode.id,
 							name = episode.name,
 							overview = episode.overview,
 							imageUrl = imageUrl,
 							episodeNumber = episode.episodeNumber,
 							seasonNumber = episode.seasonNumber,
 							airDate = episode.airDate,
 							isMissing = episode.missing,
 							isAvailable = isEpisodeAvailable,
 							jellyfinId = jellyfinEpisodeId,
 						)
 					}
 				}
 			}.onFailure {
 				Timber.e(it, "Failed to load Jellyseerr episodes for tv $tmdbId season $seasonNumber")
 			}
 		}

	override suspend fun getPersonDetails(personId: Int): Result<JellyseerrPersonDetails> =
    withContext(Dispatchers.IO) {
        val config = getConfig()
            ?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

        val url = "${config.baseUrl}/api/v1/person/$personId"
        val request = Request.Builder()
            .url(url)
            .header("X-API-Key", config.apiKey)
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
                val body = response.body?.string() ?: throw IllegalStateException("Empty body")
                val raw = json.decodeFromString(JellyseerrPersonDetails.serializer(), body)

                raw.copy(
                    profilePath = profileImageUrl(raw.profilePath),
                )
            }
        }.onFailure {
            Timber.e(it, "Failed to load Jellyseerr person details")
        }
    }

	override suspend fun getPersonCredits(personId: Int): Result<List<JellyseerrSearchItem>> =
		withContext(Dispatchers.IO) {
			val config = getConfig()
				?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

			val url = "${config.baseUrl}/api/v1/person/$personId/combined_credits"
			val request = Request.Builder()
				.url(url)
				.header("X-API-Key", config.apiKey)
				.build()

			runCatching {
				client.newCall(request).execute().use { response ->
					if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
					val body = response.body?.string() ?: throw IllegalStateException("Empty body")
					val combined = json.decodeFromString(JellyseerrCombinedCredits.serializer(), body)

					combined.cast
						.filter { it.mediaType == "movie" || it.mediaType == "tv" }
						.map { credit ->
							val posterUrl = posterImageUrl(credit.posterPath)
							val backdropUrl = backdropImageUrl(credit.backdropPath)
							JellyseerrSearchItem(
								id = credit.id,
								mediaType = credit.mediaType ?: "movie",
								title = (credit.title ?: credit.name).orEmpty(),
								overview = credit.overview,
								posterPath = posterUrl,
								backdropPath = backdropUrl,
							)
						}
				}
			}.onFailure {
				Timber.e(it, "Failed to load Jellyseerr person credits")
			}
		}
}
