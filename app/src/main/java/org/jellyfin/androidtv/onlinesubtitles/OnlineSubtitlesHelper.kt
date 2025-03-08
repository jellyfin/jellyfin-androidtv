package org.jellyfin.androidtv.onlinesubtitles

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jellyfin.androidtv.onlinesubtitles.opensubtitles.OpenSubtitlesSource
import org.jellyfin.androidtv.onlinesubtitles.subdl.SubdlOnlineSubtitleSource
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.io.File
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter


interface OnlineSubtitlesHelper {

	fun downloadSubtitles(viewLifecycleOwner: LifecycleOwner, baseItemDto: BaseItemDto)

	suspend fun getSubtitleFile(context: Context, subtitle: OnlineSubtitle, showInfo: (infoText: String) -> Unit): Result<File>

	/***
	 * This method is used when adding subtitle track
	 */
	fun findOnlineSubtitle(id: UUID, index: Int): OnlineSubtitle?
	fun getSubtitlesForMedia(id: UUID): List<OnlineSubtitle>
	fun getDownloadedFile(context: Context, fileName: String): File
	fun addOffsetToSubtitle(context: Context, id: UUID, index: Int, diff: Long): OnlineSubtitle?
}

class OnlineSubtitlesHelperImpl(
	val userPreferences: UserPreferences,
	val openSubtitlesSource: OpenSubtitlesSource,
	val subdlSource: SubdlOnlineSubtitleSource
) :
	OnlineSubtitlesHelper {

	fun subtitlesSourceList(): List<OnlineSubtitleSource> {
		return listOf(openSubtitlesSource, subdlSource)
	}

	val cacheMap = HashMap<UUID, List<OnlineSubtitle>>()

	private fun setSubtitlesForMedia(id: UUID, fpsCompatibleList: List<OnlineSubtitle>) {
		cacheMap[id] = fpsCompatibleList
	}

	val SUBTITLE_DIRNAME = "onlinesubtitles"

	override fun getDownloadedFile(context: Context, fileName: String): File {
		val dir = File(context.filesDir, SUBTITLE_DIRNAME)
		if (!dir.exists()) dir.mkdirs()

		val file = File(dir, fileName)
		return file
	}

	override fun getSubtitlesForMedia(id: UUID): List<OnlineSubtitle> {
		return cacheMap[id].orEmpty()
	}

	override fun findOnlineSubtitle(id: UUID, index: Int): OnlineSubtitle? {
		return cacheMap[id].orEmpty().firstOrNull { it.localSubtitleId == index }
	}

	override fun downloadSubtitles(viewLifecycleOwner: LifecycleOwner, baseItemDto: BaseItemDto) {

		if (!cacheMap[baseItemDto.id].isNullOrEmpty()) {
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


			val seasonNumber = when {
				baseItemDto.type == BaseItemKind.EPISODE
					&& baseItemDto.parentIndexNumber != null
					&& baseItemDto.parentIndexNumber != 0 ->
					baseItemDto.parentIndexNumber

				else -> null
			}

			val episodeNumber: Int? = when {
				baseItemDto.type != BaseItemKind.EPISODE -> baseItemDto.indexNumber
				baseItemDto.parentIndexNumber == 0 -> null
				else -> baseItemDto.indexNumber?.let { start ->
					baseItemDto.indexNumberEnd?.let { end -> null }
						?: start
				}
			}

			subtitlesSourceList().forEach {
				it.fetchSubtitleList(baseItemDto, tmdbId, tmdbStr, imdbId, imdbStr,  fps, seasonNumber, episodeNumber){ id,type,list->
					setSubtitlesForMedia(id, list)
				}
			}


			val results = mutableListOf<OnlineSubtitle>()
			coroutineScope {
				val deferredResults = subtitlesSourceList().map { source ->
					async {
						withTimeoutOrNull(10000){
							source.fetchSubtitleList(
								baseItemDto,
								tmdbId,
								tmdbStr,
								imdbId,
								imdbStr,
								fps,
								seasonNumber,
								episodeNumber
							) { id, type, list ->
								synchronized(results) {
									results.addAll(list)
								}
							}
						}
					}
				}

				deferredResults.awaitAll()
			}

			setSubtitlesForMedia(baseItemDto.id, results)
		}
	}

	override suspend fun getSubtitleFile(context: Context, subtitle: OnlineSubtitle, showInfo: (infoText: String) -> Unit): Result<File> {
		val downloadedFile = getDownloadedFile(context, subtitle.localFileName)
		if (downloadedFile.exists()) {
			return Result.success(downloadedFile)
		} else {
			//file is not exist, so make an download call
			val key = subtitle.type
			return subtitlesSourceList().first { it.type == key}.downloadSubtitle(subtitle, downloadedFile, showInfo)
		}
	}

	override fun addOffsetToSubtitle(context: Context, id: UUID, index: Int, diff: Long): OnlineSubtitle? {
		val currentSubtitle = findOnlineSubtitle(id, index) ?: return null

		val isVariant = currentSubtitle.offset != 0L

		val cleanedFileName = currentSubtitle.localFileName.replace("\\(\\+\\d+\\)\\.srt$|\\.srt$".toRegex(), "")

		val newOffset = currentSubtitle.offset + diff
		val sign = if (newOffset >= 0) "+" else ""
		val newFileName = "$cleanedFileName($sign$newOffset).srt"


		val newSubtitle = OnlineSubtitle(
			title = currentSubtitle.title,
			language = currentSubtitle.language,
			localSubtitleId = OnlineSubtitleIndexer.generateUniqueId(),
			localFileName = newFileName,
			type = currentSubtitle.type,
			downloadParamLong1 = currentSubtitle.downloadParamLong1,
			downloadParamInt1 = currentSubtitle.downloadParamInt1,
			downloadParamStr1 = currentSubtitle.downloadParamStr1,
			offset = newOffset
		)

		val inputFile = getDownloadedFile(context, currentSubtitle.localFileName)
		val outputFile = getDownloadedFile(context, newFileName)

		if (!outputFile.exists()){
			applySrtDelay(inputFile, outputFile, diff)
		}

		//update list with new subtitle
		val list = getSubtitlesForMedia(id).toMutableList()
		if (isVariant){
			//find and remove variant, and maybe delete file?
			list.removeIf { currentSubtitle.localSubtitleId == it.localSubtitleId }
		}

		list.add(newSubtitle)
		cacheMap[id] = list


		return newSubtitle
	}



	private fun srtTimeToMillis(timeStr: String): Long {
		val formatter = DateTimeFormatter.ofPattern("HH:mm:ss,SSS")
		val time = LocalTime.parse(timeStr, formatter)
		return Duration.between(LocalTime.MIN, time).toMillis()
	}

	private fun millisToSrtTime(millis: Long): String {
		val time = LocalTime.MIN.plus(Duration.ofMillis(millis))
		return time.format(DateTimeFormatter.ofPattern("HH:mm:ss,SSS"))
	}

	private fun applySrtDelay(input: File, output: File, delayMs: Long) {
		val regex = Regex("(\\d{2}:\\d{2}:\\d{2},\\d{3}) --> (\\d{2}:\\d{2}:\\d{2},\\d{3})")

		output.bufferedWriter().use { writer ->
			input.forEachLine { line ->
				val match = regex.find(line)
				if (match != null) {
					val newStart = millisToSrtTime(srtTimeToMillis(match.groupValues[1]) + delayMs)
					val newEnd = millisToSrtTime(srtTimeToMillis(match.groupValues[2]) + delayMs)
					writer.write("$newStart --> $newEnd\n")
				} else {
					writer.write("$line\n")
				}
			}
		}
	}
}
