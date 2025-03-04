package org.jellyfin.androidtv.onlinesubtitles.subdl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jellyfin.androidtv.preference.UserPreferences
import org.json.JSONObject
import timber.log.Timber

interface SubdlClient {

	suspend fun getSubtitles(
		customApiKey: String,
		params: SubtitleQueryParams,
	): Result<SubdlSubtitlesResponse>

	suspend fun getSupportedLanguages(): Result<Map<String, String>>

}

internal val defaultApiKey = "-U9yRY2FIwVDIcOz1fwyy-un7hg9tawL"


class SubdlClientImpl(val userPreferences: UserPreferences) : SubdlClient {


	private val client = OkHttpClient()
	private val json = Json { ignoreUnknownKeys = true }


	private suspend inline fun <reified T> makeRequest(
		url: String,
		method: String,
		body: JSONObject? = null,
	): Result<T> {
		return withContext(Dispatchers.IO) {
			try {
				val requestBody = body?.toString()?.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
				val requestBuilder = Request.Builder()
					.url(url)
					.addHeader("Accept", "application/json")

				when (method) {
					"POST" -> requestBuilder.post(requestBody!!)
					"GET" -> requestBuilder.get()
					"DELETE" -> requestBuilder.delete()
				}

				val response = client.newCall(requestBuilder.build()).execute()
				val jsonResponse = response.body?.string()
				Timber.tag("SUBDL").d("Response: ${jsonResponse}")

				if (jsonResponse.isNullOrEmpty()) {
					return@withContext Result.failure(Exception("Empty response from SUBDL"))
				}

				return@withContext if (response.isSuccessful) {
					try {
						val data: T = json.decodeFromString(jsonResponse)
						Result.success(data)
					} catch (e: Exception) {
						Timber.tag("SUBDL").e("Technical Error: ${e.message}")
						Result.failure(Exception("Technical Error: ${e.message}"))
					}
				} else {
					try {
						val errorData: OS_ErrorResponse = json.decodeFromString(jsonResponse)
						Result.failure(Exception("Error: ${errorData.message}"))
					} catch (e: Exception) {
						Timber.tag("SUBDL").e("Technical Error: ${e.message}")
						Result.failure(Exception("Technical Error:  ${e.message}"))
					}
				}
			} catch (e: Exception) {
				Result.failure(e)
			}
		}
	}

	override suspend fun getSubtitles(
		customApiKey: String,
		params: SubtitleQueryParams,
	): Result<SubdlSubtitlesResponse> {
		val url = "https://api.subdl.com/api/v1/subtitles?" +
			params.toQueryMap().entries.joinToString("&") { "${it.key}=${it.value}" }

		return makeRequest(url, "GET")
	}

	override suspend fun getSupportedLanguages(
	): Result<Map<String, String>> {
		return makeRequest(
			url = "https://subdl.com/api-files/language_list.json",
			method = "GET",
		)
	}


}

@Serializable
data class OS_ErrorResponse(
	val message: String? = null,
)


data class SubtitleQueryParams(
	val customApiKey: String? = null,  // Required: API key from subdl account
	val film_name: String? = null, // Optional: Text search by film name
	val file_name: String? = null, // Optional: Search by file name
	val sd_id: String? = null, // Optional: Search by SubDL ID
	val imdb_id: String? = null, // Optional: Search by IMDb ID
	val tmdb_id: String? = null, // Optional: Search by TMDB ID
	val season_number: Int? = null, // Optional: Specific season number for TV shows
	val episode_number: Int? = null, // Optional: Specific episode number for TV shows
	val type: String? = null, // Optional: Type of the content, either movie or tv
	val year: Int? = null, // Optional: Release year of the movie or TV show
	val languages: String? = null, // Optional: Comma-separated language codes for subtitle languages
	val subs_per_page: Int? = 10, // Optional: Limit of subtitles to see in results, default 10 (max 30)
	val comment: Int? = null, // Optional: Send comment=1 to get author comment on subtitle
	val releases: Int? = null, // Optional: Send releases=1 to get releases list on subtitle
	val hi: Int? = null, // Optional: Send hi=1 to get "Hearing Impaired" subtitles
	val full_season: Int? = null, // Optional: Send full_season=1 to get all full season subtitles
) {
	// Method to convert data class to query map
	fun toQueryMap(): Map<String, String> {
		val map = mutableMapOf<String, String>()

		map["api_key"] = if (customApiKey.isNullOrEmpty()) defaultApiKey else customApiKey

		// Add optional parameters only if they are non-null
		film_name?.let { map["film_name"] = it }
		file_name?.let { map["file_name"] = it }
		sd_id?.let { map["sd_id"] = it }
		imdb_id?.let { map["imdb_id"] = it }
		tmdb_id?.let { map["tmdb_id"] = it }
		season_number?.let { map["season_number"] = it.toString() }
		episode_number?.let { map["episode_number"] = it.toString() }
		type?.let { map["type"] = it }
		year?.let { map["year"] = it.toString() }
		languages?.let {
			val preferredLang: String? = it.ifEmpty { null }
			preferredLang?.let { l ->
				map["languages"] = l
			}
		}
		subs_per_page?.let { map["subs_per_page"] = it.toString() }
		comment?.let { map["comment"] = it.toString() }
		releases?.let { map["releases"] = it.toString() }
		hi?.let { map["hi"] = it.toString() }
		full_season?.let { map["full_season"] = it.toString() }

		return map
	}
}


// Subtitle sınıfı
@Serializable
data class SubdlSubtitle(
	val release_name: String? = null,
	val name: String? = null,
	val lang: String? = null,
	val language: String? = null,
	val author: String? = null,
	val url: String? = null,
	val subtitlePage: String? = null,
	val season: Int? = null,
	val episode: Int? = null,
	val hi: Boolean? = null,
	val full_season: Boolean? = null,
	) {



	fun localFileName(): String {
		val fileName = url!!.substringAfterLast("/")

		return fileName.replace(".zip", ".srt")
	}
}

// OpenSubtitlesResponse sınıfı
@Serializable
data class SubdlSubtitlesResponse(
	val status: Boolean?= null,
	val results: List<SubdlResultItem>?= null,
	val subtitles: List<SubdlSubtitle>?= null,
	val totalPages: Int? = null,
	val currentPage: Int? = null,
)

// ResultItem sınıfı
@Serializable
data class SubdlResultItem(
	val sd_id: Int? = null,
	val type: String? = null,
	val name: String? = null,
	val imdb_id: String?,
	val tmdb_id: Int? = null,
	val year: Int? = null,
	val slug: String? = null,
)


