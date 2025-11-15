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
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber

interface JellyseerrRepository {
	suspend fun search(query: String): Result<List<JellyseerrSearchItem>>
	suspend fun getOwnRequests(): Result<List<JellyseerrRequest>>
	suspend fun getRecentRequests(): Result<List<JellyseerrRequest>>
	suspend fun createRequest(item: JellyseerrSearchItem, seasons: List<Int>? = null): Result<Unit>
	suspend fun discoverTrending(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun discoverMovies(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun discoverTv(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun discoverUpcomingMovies(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun discoverUpcomingTv(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun getMovieDetails(tmdbId: Int): Result<JellyseerrMovieDetails>
	suspend fun getTvDetails(tmdbId: Int): Result<JellyseerrMovieDetails>
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
	val isRequested: Boolean = false,
	val isAvailable: Boolean = false,
	val requestId: Int? = null,
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
)

@Serializable
data class JellyseerrMovieDetails(
	val id: Int,
	val title: String? = null,
	val originalTitle: String? = null,
	val overview: String? = null,
	val posterPath: String? = null,
	val backdropPath: String? = null,
	val releaseDate: String? = null,
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
	private data class Config(val baseUrl: String, val apiKey: String)

	private val client = OkHttpClient()
	private val json = Json {
		ignoreUnknownKeys = true
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
					if (item.isAvailable || item.mediaType != "movie") return@map item

					val title = item.title.trim()
					if (title.isEmpty()) return@map item

					val request = GetItemsRequest(
						searchTerm = title,
						limit = 5,
						includeItemTypes = setOf(BaseItemKind.MOVIE),
						recursive = true,
						enableTotalRecordCount = false,
					)

					val resultItems = try {
						apiClient.itemsApi.getItems(request).content.items
					} catch (error: ApiClientException) {
						Timber.w(error, "Failed to query Jellyfin for availability of \"%s\"", title)
						return@map item
					}

					val matches = resultItems?.any { baseItem ->
						val name = baseItem.name?.trim().orEmpty()
						val original = baseItem.originalTitle?.trim().orEmpty()
						name.equals(title, ignoreCase = true) ||
							original.equals(title, ignoreCase = true)
					} == true

					if (matches) item.copy(isAvailable = true) else item
				}
			}.onFailure {
				Timber.e(it, "Failed to mark Jellyseerr items as available in Jellyfin")
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

				val baseImageUrl = "https://image.tmdb.org/t/p/w500"

				result.results
					.filter { it.mediaType == "movie" || it.mediaType == "tv" }
					.map {
						val posterUrl = it.posterPath?.let { path -> "$baseImageUrl$path" }
						val backdropUrl = it.backdropPath?.let { path -> "$baseImageUrl$path" }
						JellyseerrSearchItem(
							id = it.id,
							mediaType = it.mediaType,
							title = (it.title ?: it.name).orEmpty(),
							overview = it.overview,
							posterPath = posterUrl,
							backdropPath = backdropUrl,
						)
					}
			}
		}.onFailure {
			Timber.e(it, "Failed to search Jellyseerr")
		}
	}

	override suspend fun getOwnRequests(): Result<List<JellyseerrRequest>> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }
		val baseImageUrl = "https://image.tmdb.org/t/p/w500"

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
					val posterUrl = dto.media?.posterPath?.let { path -> "$baseImageUrl$path" }
					val backdropUrl = dto.media?.backdropPath?.let { path -> "$baseImageUrl$path" }
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

	override suspend fun getRecentRequests(): Result<List<JellyseerrRequest>> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }
		val baseImageUrl = "https://image.tmdb.org/t/p/w500"

		val url = "${config.baseUrl}/api/v1/request?filter=all&take=10&sort=modified&skip=0"
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
					val posterUrl = dto.media?.posterPath?.let { path -> "$baseImageUrl$path" }
					val backdropUrl = dto.media?.backdropPath?.let { path -> "$baseImageUrl$path" }
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

				val baseImageUrl = "https://image.tmdb.org/t/p/w500"

				result.results
					.filter { it.mediaType == "movie" || it.mediaType == "tv" }
					.map {
						val posterUrl = it.posterPath?.let { path -> "$baseImageUrl$path" }
						val backdropUrl = it.backdropPath?.let { path -> "$baseImageUrl$path" }
						JellyseerrSearchItem(
							id = it.id,
							mediaType = it.mediaType,
							title = (it.title ?: it.name).orEmpty(),
							overview = it.overview,
							posterPath = posterUrl,
							backdropPath = backdropUrl,
						)
					}
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

				val baseImageUrl = "https://image.tmdb.org/t/p/w500"

				result.results
					.filter { it.mediaType == "movie" || it.mediaType == "tv" }
					.map {
						val posterUrl = it.posterPath?.let { path -> "$baseImageUrl$path" }
						val backdropUrl = it.backdropPath?.let { path -> "$baseImageUrl$path" }
						JellyseerrSearchItem(
							id = it.id,
							mediaType = it.mediaType,
							title = (it.title ?: it.name).orEmpty(),
							overview = it.overview,
							posterPath = posterUrl,
							backdropPath = backdropUrl,
						)
					}
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

				val baseImageUrl = "https://image.tmdb.org/t/p/w500"

				result.results
					.filter { it.mediaType == "movie" || it.mediaType == "tv" }
					.map {
						val posterUrl = it.posterPath?.let { path -> "$baseImageUrl$path" }
						val backdropUrl = it.backdropPath?.let { path -> "$baseImageUrl$path" }
						JellyseerrSearchItem(
							id = it.id,
							mediaType = it.mediaType,
							title = (it.title ?: it.name).orEmpty(),
							overview = it.overview,
							posterPath = posterUrl,
							backdropPath = backdropUrl,
						)
					}
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

				val baseImageUrl = "https://image.tmdb.org/t/p/w500"

				result.results
					.filter { it.mediaType == "movie" || it.mediaType == "tv" }
					.map {
						val posterUrl = it.posterPath?.let { path -> "$baseImageUrl$path" }
						val backdropUrl = it.backdropPath?.let { path -> "$baseImageUrl$path" }
						JellyseerrSearchItem(
							id = it.id,
							mediaType = it.mediaType,
							title = (it.title ?: it.name).orEmpty(),
							overview = it.overview,
							posterPath = posterUrl,
							backdropPath = backdropUrl,
						)
					}
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

				val baseImageUrl = "https://image.tmdb.org/t/p/w500"

				result.results
					.filter { it.mediaType == "movie" || it.mediaType == "tv" }
					.map {
						val posterUrl = it.posterPath?.let { path -> "$baseImageUrl$path" }
						val backdropUrl = it.backdropPath?.let { path -> "$baseImageUrl$path" }
						JellyseerrSearchItem(
							id = it.id,
							mediaType = it.mediaType,
							title = (it.title ?: it.name).orEmpty(),
							overview = it.overview,
							posterPath = posterUrl,
							backdropPath = backdropUrl,
						)
					}
			}
		}.onFailure {
			Timber.e(it, "Failed to load Jellyseerr upcoming tv")
		}
	}

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
				val baseImageUrl = "https://image.tmdb.org/t/p/w500"

				val mappedCredits = raw.credits?.let { credits ->
					val mappedCast = credits.cast.map { castMember ->
						val profile = castMember.profilePath?.let { path -> baseImageUrl + path }
						castMember.copy(profilePath = profile)
					}
					credits.copy(cast = mappedCast)
				}

				raw.copy(
					posterPath = raw.posterPath?.let { baseImageUrl + it },
					backdropPath = raw.backdropPath?.let { baseImageUrl + it },
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
				val baseImageUrl = "https://image.tmdb.org/t/p/w500"

				val mappedCredits = raw.credits?.let { credits ->
					val mappedCast = credits.cast.map { castMember ->
						val profile = castMember.profilePath?.let { path -> baseImageUrl + path }
						castMember.copy(profilePath = profile)
					}
					credits.copy(cast = mappedCast)
				}

				val mappedSeasons = raw.seasons.map { season ->
					season.copy(
						posterPath = season.posterPath?.let { path -> baseImageUrl + path },
					)
				}

				JellyseerrMovieDetails(
					id = raw.id,
					title = raw.name,
					originalTitle = null,
					overview = raw.overview,
					posterPath = raw.posterPath?.let { baseImageUrl + it },
					backdropPath = raw.backdropPath?.let { baseImageUrl + it },
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
                val baseImageUrl = "https://image.tmdb.org/t/p/w500"

                raw.copy(
                    profilePath = raw.profilePath?.let { baseImageUrl + it },
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

					val baseImageUrl = "https://image.tmdb.org/t/p/w500"

					combined.cast
						.filter { it.mediaType == "movie" || it.mediaType == "tv" }
						.map { credit ->
							val posterUrl = credit.posterPath?.let { path -> baseImageUrl + path }
							val backdropUrl = credit.backdropPath?.let { path -> baseImageUrl + path }
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
