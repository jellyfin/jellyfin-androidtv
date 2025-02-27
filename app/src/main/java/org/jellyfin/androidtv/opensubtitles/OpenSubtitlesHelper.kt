package org.jellyfin.androidtv.opensubtitles

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.opensubtitles.OpenSubtitlesHelper.Companion.getSubtitleDownloadedFile
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber
import java.io.File
import java.io.IOException


interface OpenSubtitlesHelper {

	fun isLoggedIn() : Boolean

	fun getSubtitles(viewLifecycleOwner: LifecycleOwner, baseItemDto: BaseItemDto)

	suspend fun getSubtitleFile(context: Context, subtitle: OS_Subtitle, showInfo: (infoText: String) -> Unit): Result<File>

	companion object {
		fun getSubtitleDownloadedFile(context: Context, subtitleId: String): File {
			val dir = File(context.filesDir, "opensubtitles")
			if (!dir.exists()) dir.mkdirs()

			val fileName = "$subtitleId.srt"
			val file = File(dir, fileName)
			return file
		}
	}

}

class OpenSubtitlesHelperImpl(val openSubtitlesClient: OpenSubtitlesClient, val userPreferences: UserPreferences) : OpenSubtitlesHelper {

	override fun isLoggedIn(): Boolean {
		val token = userPreferences[UserPreferences.openSubtitlesToken]
		return token.isNotEmpty()
	}
	override fun getSubtitles(viewLifecycleOwner: LifecycleOwner, baseItemDto: BaseItemDto) {

		if (!OpenSubtitlesCache.getSubtitlesForMedia(baseItemDto.id).isNullOrEmpty()) {
			return
		}

		viewLifecycleOwner.lifecycleScope.launch {

			val data: Map<String, String?>? = baseItemDto.providerIds

			val tmdbStr = data?.getOrDefault("Tmdb", null)
			val imdbStr = data?.getOrDefault("Imdb", null)

			var tmdbId: Int? = null
			var imdbId: Int? = null

			try {
				tmdbId = Integer.parseInt(tmdbStr!!)
			} catch (_: Exception) {
			}

			try {
				//tt0050083
				imdbId = Integer.parseInt(imdbStr!!.substring(2))
			} catch (_: Exception) {
			}

			val fps = baseItemDto.mediaStreams?.get(0)?.averageFrameRate

			val token = userPreferences[UserPreferences.openSubtitlesToken]
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

				val seasonNumber = when {
					baseItemDto.type == BaseItemKind.EPISODE
						&& baseItemDto.parentIndexNumber != null
						&& baseItemDto.parentIndexNumber != 0 ->
						baseItemDto.parentIndexNumber
					else -> null
				}

				val episodeNumber : Int? = when {
					baseItemDto.type != BaseItemKind.EPISODE -> baseItemDto.indexNumber
					baseItemDto.parentIndexNumber == 0 -> null
					else -> baseItemDto.indexNumber?.let { start ->
						baseItemDto.indexNumberEnd?.let { end -> null }
							?: start
					}
				}
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
						kotlin.math.abs(diff) < 0.001
					}
					filteredList.ifEmpty { list }
				} ?: emptyList()

				OpenSubtitlesCache.setSubtitlesForMedia(baseItemDto.id, fpsCompatibleList)

			}.onFailure {
				Timber.tag("OpenSubtitlesHelper").d("fail : %s", it)
			}
		}


	}

	override suspend fun getSubtitleFile(context: Context, subtitle: OS_Subtitle, showInfo: (infoText: String) -> Unit): Result<File> {
		val file = getSubtitleDownloadedFile(context, subtitle.id!!)
		if (file.exists()) {
			return Result.success(file)
		} else {
			//file is not exist, so make an opensubtitles.com download call

			val token = userPreferences[UserPreferences.openSubtitlesToken]
			val customApiKey = userPreferences[UserPreferences.openSubtitlesCustomApiKey]
			val customUserAgent = userPreferences[UserPreferences.openSubtitlesCustomUserAgent]

			val result =
				openSubtitlesClient.downloadSubtitle(token, customApiKey, customUserAgent, subtitle.attributes!!.files!!.first().fileId!!)
			result.onSuccess { data ->

				showInfo("⬇\uFE0F" + "Remaining: " + data.remaining + "⏳" + " Until: " + data.reset_time + " ✉\uFE0F " + data.message)
				//start download
				val downloadedFile = try {
					getSubtitleFile(context, subtitle.localFileName(), data.link!!)
				} catch (e: Exception) {
					return Result.failure(e)
				}
				return Result.success(downloadedFile)

			}.onFailure {
				showInfo("⛔" + it.message)

				Timber.tag("OpenSubtitlesHelper").e("fail : %s", it)
				return Result.failure(it)
			}


		}

		return Result.failure(RuntimeException())

	}


	suspend fun getSubtitleFile(context: Context, fileName: String, fileUrl: String): File {
		val dir = File(context.filesDir, "opensubtitles")
		if (!dir.exists()) dir.mkdirs()

		val file = File(dir, fileName)
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
