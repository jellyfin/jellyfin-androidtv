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
import timber.log.Timber

interface JellyseerrRepository {
	suspend fun search(query: String): Result<List<JellyseerrSearchItem>>
	suspend fun getOwnRequests(): Result<List<JellyseerrRequest>>
	suspend fun createRequest(item: JellyseerrSearchItem): Result<Unit>
	suspend fun discoverMovies(page: Int = 1): Result<List<JellyseerrSearchItem>>
	suspend fun getMovieDetails(tmdbId: Int): Result<JellyseerrMovieDetails>
	suspend fun cancelRequest(requestId: Int): Result<Unit>
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
)

@Serializable
data class JellyseerrGenre(
	val id: Int,
	val name: String,
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
)

class JellyseerrRepositoryImpl(
	private val userPreferences: UserPreferences,
	private val sessionRepository: SessionRepository,
	private val userRepository: UserRepository,
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

	override suspend fun createRequest(item: JellyseerrSearchItem): Result<Unit> = withContext(Dispatchers.IO) {
		val config = getConfig()
			?: return@withContext Result.failure(IllegalStateException("Jellyseerr not configured"))

		val userId = resolveCurrentUserId(config).getOrElse { return@withContext Result.failure(it) }

		val body = JellyseerrCreateRequestBody(
			mediaType = item.mediaType,
			mediaId = item.id,
			userId = userId,
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

				raw.copy(
					posterPath = raw.posterPath?.let { baseImageUrl + it },
					backdropPath = raw.backdropPath?.let { baseImageUrl + it },
				)
			}
		}.onFailure {
			Timber.e(it, "Failed to load Jellyseerr movie details")
		}
	}
}
