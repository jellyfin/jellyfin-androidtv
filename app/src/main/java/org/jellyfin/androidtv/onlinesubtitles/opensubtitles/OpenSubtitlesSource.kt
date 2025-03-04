package org.jellyfin.androidtv.onlinesubtitles.opensubtitles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jellyfin.androidtv.onlinesubtitles.OnlineSubtitle
import org.jellyfin.androidtv.onlinesubtitles.OnlineSubtitleIndexer
import org.jellyfin.androidtv.onlinesubtitles.OnlineSubtitleSource
import org.jellyfin.androidtv.onlinesubtitles.OnlineSubtitleType
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.abs

class OpenSubtitlesSource(val openSubtitlesClient: OpenSubtitlesClient, val userPreferences: UserPreferences) : OnlineSubtitleSource {

	override val type: OnlineSubtitleType
		get() = OnlineSubtitleType.OPENSUBTITLES

	override suspend fun fetchSubtitleList(
		baseItemDto: BaseItemDto,
		tmdbId: Int?,
		tmdbStr: String?,
		imdbId: Int?,
		imdbStr: String?,
		fps: Float?,
		seasonNumber: Int?,
		episodeNumber: Int?,
		setSubtitlesForMedia: (mediaId: UUID, type: OnlineSubtitleType, list: List<OnlineSubtitle>) -> Unit,
	) {
		val token = userPreferences[UserPreferences.openSubtitlesToken]
		val isLoggedIn = token.isNotEmpty()
		if (!isLoggedIn)
			return

		val customApiKey = userPreferences[UserPreferences.openSubtitlesCustomApiKey]
		val customUserAgent = userPreferences[UserPreferences.openSubtitlesCustomUserAgent]
		val preferredLanguages = userPreferences[UserPreferences.openSubtitlesPreferredLanguages]

		val params = if (baseItemDto.type.toString() == "Movie") {
			//movie
			SubtitleQueryParams(
				query = baseItemDto.mediaSources?.get(0)?.name ?: baseItemDto.name,
				languages = preferredLanguages,
				tmdbId = tmdbId,
				imdbId = imdbId,
			)
		} else {
			//series

			SubtitleQueryParams(
				query = baseItemDto.mediaSources?.get(0)?.name ?: baseItemDto.name,
				languages = preferredLanguages,
				parentTmdbId = tmdbId,
				parentImdbId = imdbId,
				episodeNumber = episodeNumber,
				seasonNumber = seasonNumber,
			)
		}

		openSubtitlesClient.getSubtitles(token, customApiKey, customUserAgent, params).onSuccess { successData ->
			Timber.tag("OpenSubtitlesHelper").d("size : %s", successData.data?.size)

			val fpsCompatibleList: List<OS_Subtitle> = successData.data?.let { list ->
				val filteredList = list.filter {
					val diff: Double = it.attributes?.fps?.minus(fps!!) ?: 0.0
					abs(diff) < 0.001
				}
				filteredList.ifEmpty { list }
			} ?: emptyList()

			val resultList = fpsCompatibleList.mapNotNull {
				try {
					OnlineSubtitle(
						title = it.attributes!!.release!! + " - OpenSubtitles.com",
						language = it.attributes.language!!,
						localSubtitleId = OnlineSubtitleIndexer.generateUniqueId(),
						localFileName = "${it.id}.srt",
						type = type,
						downloadParamLong1 = it.attributes.files!!.first().fileId
					)
				} catch (e: Exception) {
					null
				}
			}

			setSubtitlesForMedia(baseItemDto.id, type, resultList)

		}.onFailure {
			Timber.tag("OpenSubtitlesHelper").d("fail : %s", it)
		}
	}

	override suspend fun downloadSubtitle(subtitle: OnlineSubtitle, fileToWrite: File, showInfo: (infoText: String) -> Unit): Result<File> {

		val token = userPreferences[UserPreferences.openSubtitlesToken]
		val customApiKey = userPreferences[UserPreferences.openSubtitlesCustomApiKey]
		val customUserAgent = userPreferences[UserPreferences.openSubtitlesCustomUserAgent]

		val result =
			openSubtitlesClient.downloadSubtitle(token, customApiKey, customUserAgent, subtitle.downloadParamLong1!!)
		result.onSuccess { data ->

			showInfo("⬇\uFE0F" + "Remaining: " + data.remaining + "⏳" + " Until: " + data.reset_time + " ✉\uFE0F " + data.message)
			//start download
			val downloadedFile = try {
				downloadFile(fileToWrite, data.link!!)
			} catch (e: Exception) {
				return Result.failure(e)
			}
			return Result.success(downloadedFile)

		}.onFailure {
			showInfo("⛔" + it.message)

			Timber.tag("OpenSubtitlesHelper").e("fail : %s", it)
			return Result.failure(it)
		}

		return Result.failure(RuntimeException())

	}



	private suspend fun downloadFile(fileToWrite: File, fileUrl: String): File {

		val file = fileToWrite
		if (file.exists()) {
			return file // Eğer dosya zaten varsa, onu döndür
		}

		return withContext(Dispatchers.IO) { // Ağ işlemi ana thread'i bloklamasın diye IO Dispatcher kullanıyoruz
			val client = OkHttpClient()
			val request = Request.Builder().url(fileUrl).build()

			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IOException("${response.code}")

				response.body?.byteStream()?.use { inputStream ->
					file.outputStream().use { outputStream ->
						inputStream.copyTo(outputStream)
					}
				}
			}

			file
		}
	}


}
