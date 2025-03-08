package org.jellyfin.androidtv.onlinesubtitles.subdl

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
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class SubdlOnlineSubtitleSource(val subdlClient: SubdlClient, val userPreferences: UserPreferences) : OnlineSubtitleSource {
	override val type: OnlineSubtitleType
		get() = OnlineSubtitleType.SUBDL

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

		val customApiKey = userPreferences[UserPreferences.subdlCustomApiKey]
		val preferredLanguages = userPreferences[UserPreferences.subdlPreferredLanguages]

		val params = if (baseItemDto.type.toString() == "Movie") {
			//movie
			SubtitleQueryParams(
				file_name = baseItemDto.mediaSources?.get(0)?.name ?: baseItemDto.name,
				tmdb_id = tmdbStr,
				imdb_id = imdbStr,
				customApiKey = customApiKey, languages = preferredLanguages
			)
		} else {
			SubtitleQueryParams(
				file_name = baseItemDto.mediaSources?.get(0)?.name ?: baseItemDto.name,
				tmdb_id = tmdbStr,
				imdb_id = imdbStr,
				season_number = seasonNumber,
				episode_number = episodeNumber,
				customApiKey = customApiKey, languages = preferredLanguages
			)
		}

		subdlClient.getSubtitles(customApiKey, params).onSuccess { successData ->
			Timber.tag("SUBDLHelper").d("size : %s", successData.subtitles?.size)

			val resultList: List<OnlineSubtitle> = successData.subtitles?.mapNotNull {
				try {

					var episodeIdentifier = ""
					if (baseItemDto.type.toString() != "Movie" && seasonNumber != null && episodeNumber != null) {
						episodeIdentifier = "S%02dE%02d".format(seasonNumber, episodeNumber)
					}

					val fileName = it.url!!.substringAfterLast("/").replace(".zip", "$episodeIdentifier.srt")

					OnlineSubtitle(
						title = it.release_name + " - SUBDL",
						language = it.language!!,
						localSubtitleId = OnlineSubtitleIndexer.generateUniqueId(),
						localFileName = fileName,
						type = type,
						downloadParamStr1 = it.url,
						downloadParamInt1 = episodeNumber

					)
				} catch (e: Exception) {
					null
				}
			} ?: emptyList()

			setSubtitlesForMedia(baseItemDto.id, type, resultList)

		}.onFailure {
			Timber.tag("SUBDLHelper").d("fail : %s", it)
		}
	}

	override suspend fun downloadSubtitle(subtitle: OnlineSubtitle, fileToWrite: File, showInfo: (infoText: String) -> Unit): Result<File> {

		val fullUrl = "https://dl.subdl.com${subtitle.downloadParamStr1}"

		val zipFileToWrite = File(fileToWrite.parentFile, subtitle.localFileName + ".zip")
		val downloadedZipFile = downloadFile(zipFileToWrite, fullUrl)

		Timber.tag("SUBDLHelper").d("downloadedZipFile : %s", downloadedZipFile)

		return try {
			val zipExtractedSuccessfully = if (subtitle.downloadParamInt1 != null) extractEpisodeFileFromZip(
				downloadedZipFile,
				fileToWrite,
				subtitle.downloadParamInt1
			) else extractFirstFileFromZip(downloadedZipFile, fileToWrite)

			if (zipExtractedSuccessfully) {
				Timber.tag("SUBDLHelper").d("extractFileFromZip success : %s", subtitle.localFileName)

				Result.success(fileToWrite)
			} else {
				Timber.tag("SUBDLHelper").d("extractFileFromZip fail : %s", subtitle.localFileName)

				Result.failure(RuntimeException())
			}
		} finally {
			downloadedZipFile.delete()
		}

	}


	private suspend fun downloadFile(fileToWrite: File, fileUrl: String): File {
		Timber.tag("SUBDLHelper").d("downloadFile : %s", fileUrl)

		if (fileToWrite.exists()) {
			Timber.tag("SUBDLHelper").d("downloadFile Success Already : %s", fileUrl)
			return fileToWrite
		}

		return withContext(Dispatchers.IO) {
			val client = OkHttpClient()
			val request = Request.Builder().url(fileUrl).build()

			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) throw IOException("${response.code}")

				response.body?.byteStream()?.use { inputStream ->
					fileToWrite.outputStream().use { outputStream ->
						inputStream.copyTo(outputStream)
					}
				}
			}

			Timber.tag("SUBDLHelper").d("downloadFile Success : %s", fileUrl)

			fileToWrite
		}
	}


	private fun extractEpisodeFileFromZip(zipFile: File, fileToWrite: File, episodeNumber: Int): Boolean {
		if (!zipFile.exists() || !zipFile.canRead()) {
			Timber.tag("SUBDLHelper").d("Can't read zip file")
			return false
		}

		val formattedList = listOf(
			"E%02d".format(episodeNumber),  // E01
			"%02d".format(episodeNumber),   // 01
			episodeNumber.toString()        // 1
		)

		try {
			FileInputStream(zipFile).use { fis ->
				ZipInputStream(BufferedInputStream(fis)).use { zipInputStream ->
					val entries = mutableListOf<ZipEntry>()

					while (true) {
						val entry = zipInputStream.nextEntry ?: break
						entries.add(entry)
					}

					for (formatted in formattedList) {
						for (entry in entries) {
							if (!entry.isDirectory && entry.name.contains(formatted)) {
								println("Found matching file using $formatted: ${entry.name}")
								fileToWrite.outputStream().use { output ->
									FileInputStream(zipFile).use { fis2 ->
										ZipInputStream(BufferedInputStream(fis2)).use { zipInputStream2 ->

											while (zipInputStream2.nextEntry != null) {
												if (zipInputStream2.nextEntry.name == entry.name) {
													zipInputStream2.copyTo(output)
													return true
												}
											}
										}
									}
								}
							}
						}
					}

					if (entries.isNotEmpty()) {
						val firstEntry = entries[0]
						println("No matching file found, using first entry: ${firstEntry.name}")
						fileToWrite.outputStream().use { output ->
							FileInputStream(zipFile).use { fis2 ->
								ZipInputStream(BufferedInputStream(fis2)).use { zipInputStream2 ->
									while (zipInputStream2.nextEntry != null) {
										if (zipInputStream2.nextEntry.name == firstEntry.name) {
											zipInputStream2.copyTo(output)
											return true
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}

		return false
	}


	private fun extractFirstFileFromZip(zipFile: File, fileToWrite: File): Boolean {
		if (!zipFile.exists() || !zipFile.canRead()) {
			Timber.tag("SUBDLHelper").d("Can't read zip file")

			return false
		}

		try {
			FileInputStream(zipFile).use { fis ->
				ZipInputStream(BufferedInputStream(fis)).use { zipInputStream ->
					val firstEntry: ZipEntry? = zipInputStream.nextEntry
					if (firstEntry != null && !firstEntry.isDirectory) {
						println("First File: ${firstEntry.name}")
						fileToWrite.outputStream().use { output ->
							zipInputStream.copyTo(output)
						}

						return true
					}
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}

		return false
	}
}
