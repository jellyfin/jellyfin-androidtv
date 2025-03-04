package org.jellyfin.androidtv.onlinesubtitles.opensubtitles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jellyfin.androidtv.onlinesubtitles.OnlineSubtitleIndexer
import org.jellyfin.androidtv.preference.UserPreferences
import org.json.JSONObject
import timber.log.Timber
import java.util.Date
import java.util.UUID
import kotlin.math.absoluteValue

interface OpenSubtitlesClient {
	suspend fun login(username: String, password: String, customApiKey: String, customUserAgent: String): Result<OS_LoginResponse>

	suspend fun getUserInfo(token: String, customApiKey: String, customUserAgent: String): Result<OS_UserResponse>
	suspend fun logout(token: String, customApiKey: String, customUserAgent: String): Result<Unit>

	suspend fun getSubtitles(
		token: String,
		customApiKey: String,
		customUserAgent: String,
		params: SubtitleQueryParams,
	): Result<OS_SubtitlesResponse>

	suspend fun getSupportedLanguages(customApiKey: String, customUserAgent: String): Result<OS_SupportedLanguagesResponse>
	suspend fun downloadSubtitle(token: String, customApiKey: String, customUserAgent: String, fileId: Long): Result<OS_DownloadResponse>
}

class OpenSubtitlesClientImpl(val userPreferences: UserPreferences) : OpenSubtitlesClient {

	val defaultApiKey = "PlBFM3sO5KUTdhh2TDZO0zG78PZgqBx5"
	val defaultUserAgent = "Jellyfin_OSClient v1.0"

	private val client = OkHttpClient()
	private val json = Json { ignoreUnknownKeys = true }


	private suspend inline fun <reified T> makeRequest(
		url: String,
		method: String,
		token: String? = null,
		customApiKey: String = "",
		customUserAgent: String = "",
		body: JSONObject? = null,
	): Result<T> {
		return withContext(Dispatchers.IO) {
			try {
				val requestBody = body?.toString()?.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
				val requestBuilder = Request.Builder()
					.url(url)
					.addHeader("Accept", "application/json")
					.addHeader("User-Agent", customUserAgent.ifEmpty { defaultUserAgent })
					.addHeader("Api-Key", customApiKey.ifEmpty { defaultApiKey })

				if (token != null) {
					requestBuilder.addHeader("Authorization", "Bearer $token")
				}

				when (method) {
					"POST" -> requestBuilder.post(requestBody!!)
					"GET" -> requestBuilder.get()
					"DELETE" -> requestBuilder.delete()
				}

				val response = client.newCall(requestBuilder.build()).execute()
				val jsonResponse = response.body?.string()
				Timber.tag("OpenSubtitles").d("Response: ${jsonResponse}")

				if (jsonResponse.isNullOrEmpty()) {
					return@withContext Result.failure(Exception("Empty response from OpenSubtitles"))
				}

				return@withContext if (response.isSuccessful) {
					try {
						val data: T = json.decodeFromString(jsonResponse)
						Result.success(data)
					} catch (e: Exception) {
						Timber.tag("OpenSubtitles").e("Technical Error: ${e.message}")
						Result.failure(Exception("Technical Error: ${e.message}"))
					}
				} else {
					try {
						val errorData: OS_ErrorResponse = json.decodeFromString(jsonResponse)
						Result.failure(Exception("Error: ${errorData.message}"))
					} catch (e: Exception) {
						Timber.tag("OpenSubtitles").e("Technical Error: ${e.message}")
						Result.failure(Exception("Technical Error:  ${e.message}"))
					}
				}
			} catch (e: Exception) {
				Result.failure(e)
			}
		}
	}

	override suspend fun login(
		username: String,
		password: String,
		customApiKey: String,
		customUserAgent: String,
	): Result<OS_LoginResponse> {
		val jsonBody = JSONObject().apply {
			put("username", username)
			put("password", password)
		}
		return makeRequest("https://api.opensubtitles.com/api/v1/login", "POST", null, customApiKey, customUserAgent, jsonBody)
	}

	override suspend fun getUserInfo(token: String, customApiKey: String, customUserAgent: String): Result<OS_UserResponse> {
		return makeRequest("https://api.opensubtitles.com/api/v1/infos/user", "GET", token, customApiKey, customUserAgent)
	}

	override suspend fun logout(token: String, customApiKey: String, customUserAgent: String): Result<Unit> {
		return makeRequest("https://api.opensubtitles.com/api/v1/logout", "DELETE", token, customApiKey, customUserAgent)
	}

	override suspend fun getSubtitles(
		token: String,
		customApiKey: String,
		customUserAgent: String,
		params: SubtitleQueryParams,
	): Result<OS_SubtitlesResponse> {
		val url = "https://api.opensubtitles.com/api/v1/subtitles?" +
			params.toQueryMap().entries.joinToString("&") { "${it.key}=${it.value}" }

		return makeRequest(url, "GET", token, customApiKey, customUserAgent)
	}


	override suspend fun downloadSubtitle(
		token: String,
		customApiKey: String,
		customUserAgent: String,
		fileId: Long,
	): Result<OS_DownloadResponse> {
		val jsonBody = JSONObject().apply {
			put("file_id", fileId)
		}
		return makeRequest(
			url = "https://api.opensubtitles.com/api/v1/download",
			method = "POST",
			token = token,
			customApiKey = customApiKey,
			customUserAgent = customUserAgent,
			body = jsonBody
		)
	}

	override suspend fun getSupportedLanguages(
		customApiKey: String,
		customUserAgent: String
	): Result<OS_SupportedLanguagesResponse> {
		return makeRequest(
			url = "https://api.opensubtitles.com/api/v1/infos/languages",
			method = "GET",
			customApiKey = customApiKey,
			customUserAgent = customUserAgent
		)
	}


}


@Serializable
data class OS_User(
	@SerialName("allowed_downloads") val allowedDownloads: Int? = null,
	@SerialName("allowed_translations") val allowedTranslations: Int? = null,
	val level: String? = null,
	@SerialName("user_id") val userId: Int? = null,
	@SerialName("ext_installed") val extInstalled: Boolean? = null,
	val vip: Boolean? = null,

	@SerialName("downloads_count") val downloadsCount: Int? = null,
	@SerialName("remaining_downloads") val remainingDownloads: Int? = null,
)


@Serializable
data class OS_UserResponse(
	val data: OS_User? = null,
)


@Serializable
data class OS_LoginResponse(
	val user: OS_User? = null,
	@SerialName("base_url") val baseUrl: String? = null,
	val token: String? = null,
	val status: Int? = null,
)

@Serializable
data class OS_ErrorResponse(
	val message: String? = null,
)


data class SubtitleQueryParams(
	val aiTranslated: String? = "include",
	val episodeNumber: Int? = null,
	val foreignPartsOnly: String? = "include",
	val hearingImpaired: String? = "include",
	val id: Int? = null,
	val imdbId: Int? = null,
	val languages: String? = null,
	val machineTranslated: String? = "exclude",
	val moviehash: String? = null,
	val moviehashMatch: String? = "include",
	val orderBy: String? = null,
	val orderDirection: String? = null,
	val page: Int? = 1,
	val parentFeatureId: Int? = null,
	val parentImdbId: Int? = null,
	val parentTmdbId: Int? = null,
	val query: String? = null,
	val seasonNumber: Int? = null,
	val tmdbId: Int? = null,
	val trustedSources: String? = "include",
	val type: String? = "all",
	val uploaderId: Int? = null,
	val year: Int? = null,
) {
	fun toQueryMap(): Map<String, String> {
		val map = mutableMapOf<String, String>()

		aiTranslated?.let { map["aiTranslated"] = it }
		episodeNumber?.let { map["episodeNumber"] = it.toString() }
		foreignPartsOnly?.let { map["foreignPartsOnly"] = it }
		hearingImpaired?.let { map["hearingImpaired"] = it }
		id?.let { map["id"] = it.toString() }
		imdbId?.let { map["imdbId"] = it.toString() }
		languages?.let { map["languages"] = it }
		machineTranslated?.let { map["machineTranslated"] = it }
		moviehash?.let { map["moviehash"] = it }
		moviehashMatch?.let { map["moviehashMatch"] = it }
		orderBy?.let { map["orderBy"] = it }
		orderDirection?.let { map["orderDirection"] = it }
		page?.let { map["page"] = it.toString() }
		parentFeatureId?.let { map["parentFeatureId"] = it.toString() }
		parentImdbId?.let { map["parentImdbId"] = it.toString() }
		parentTmdbId?.let { map["parentTmdbId"] = it.toString() }
		query?.let { map["query"] = it }
		seasonNumber?.let { map["seasonNumber"] = it.toString() }
		tmdbId?.let { map["tmdbId"] = it.toString() }
		trustedSources?.let { map["trustedSources"] = it }
		type?.let { map["type"] = it }
		uploaderId?.let { map["uploaderId"] = it.toString() }
		year?.let { map["year"] = it.toString() }

		return map
	}
}

@Serializable
data class OS_DownloadResponse(
	val link: String? = null,
	val file_name: String? = null,
	val requests: Int? = null,
	val remaining: Int? = null,
	val message: String? = null,
	val reset_time: String? = null,
	val reset_time_utc: String? = null,
)

@Serializable
data class OS_Language(
	val language_code: String,
	val language_name: String
)

@Serializable
data class OS_SupportedLanguagesResponse(
	val data: List<OS_Language>
)

@Serializable
data class OS_SubtitlesResponse(
	@SerialName("total_pages")
	val totalPages: Long? = null,
	@SerialName("total_count")
	val totalCount: Long? = null,
	@SerialName("per_page")
	val perPage: Long? = null,
	val page: Long? = null,
	val data: List<OS_Subtitle>? = null,
)

@Serializable
data class OS_Subtitle(
	val id: String? = null,
	val type: String? = null,
	val attributes: OS_SubtitleAttributes? = null,
	val localId: Int = OnlineSubtitleIndexer.generateUniqueId()

)

@Serializable
data class OS_SubtitleAttributes(
	@SerialName("subtitle_id")
	val subtitleId: String? = null,
	val language: String? = null,
	@SerialName("download_count")
	val downloadCount: Long? = null,
	@SerialName("new_download_count")
	val newDownloadCount: Long? = null,
	@SerialName("hearing_impaired")
	val hearingImpaired: Boolean? = null,
	val hd: Boolean? = null,
	val fps: Double? = null,
	val votes: Long? = null,
	val ratings: Double? = null,
	@SerialName("from_trusted")
	val fromTrusted: Boolean? = null,
	@SerialName("foreign_parts_only")
	val foreignPartsOnly: Boolean? = null,
	@SerialName("upload_date")
	val uploadDate: String? = null,
	@SerialName("file_hashes")
	val fileHashes: List<String>? = null,
	@SerialName("ai_translated")
	val aiTranslated: Boolean? = null,
	@SerialName("nb_cd")
	val nbCd: Long? = null,
	val slug: String? = null,
	@SerialName("machine_translated")
	val machineTranslated: Boolean? = null,
	val release: String? = null,
	val comments: String? = null,
	@SerialName("legacy_subtitle_id")
	val legacySubtitleId: Long? = null,
	@SerialName("legacy_uploader_id")
	val legacyUploaderId: Long? = null,
	val uploader: OS_Uploader? = null,
	@SerialName("feature_details")
	val featureDetails: OS_FeatureDetails? = null,
	val url: String? = null,
	@SerialName("related_links")
	val relatedLinks: List<OS_RelatedLink>? = null,
	val files: List<OS_File>? = null,
)

@Serializable
data class OS_Uploader(
	@SerialName("uploader_id")
	val uploaderId: Long? = null,
	val name: String? = null,
	val rank: String? = null,
)

@Serializable
data class OS_FeatureDetails(
	@SerialName("feature_id")
	val featureId: Long? = null,
	@SerialName("feature_type")
	val featureType: String? = null,
	val year: Long? = null,
	val title: String? = null,
	@SerialName("movie_name")
	val movieName: String? = null,
	@SerialName("imdb_id")
	val imdbId: Long? = null,
	@SerialName("tmdb_id")
	val tmdbId: Long? = null,
)

@Serializable
data class OS_RelatedLink(
	val label: String? = null,
	val url: String? = null,
	@SerialName("img_url")
	val imgUrl: String? = null,
)

@Serializable
data class OS_File(
	@SerialName("file_id")
	val fileId: Long? = null,
	@SerialName("cd_number")
	val cdNumber: Long? = null,
	@SerialName("file_name")
	val fileName: String? = null,
)

