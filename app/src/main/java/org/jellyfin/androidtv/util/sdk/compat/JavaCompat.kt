@file:JvmName("JavaCompat")

package org.jellyfin.androidtv.util.sdk.compat

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import java.time.LocalDateTime
import java.util.UUID
import org.jellyfin.apiclient.model.dto.BaseItemDto as LegacyBaseItemdto

fun BaseItemDto.copyWithDisplayPreferencesId(
	displayPreferencesId: String?
) = copy(
	displayPreferencesId = displayPreferencesId,
)

fun BaseItemDto.copyWithDates(
	premiereDate: LocalDateTime?,
	endDate: LocalDateTime?,
	officialRating: String?,
	runTimeTicks: Long?,
) = copy(
	premiereDate = premiereDate,
	endDate = endDate,
	officialRating = officialRating,
	runTimeTicks = runTimeTicks,
)

fun BaseItemDto.copyWithTimerId(
	seriesTimerId: String?,
) = copy(
	seriesTimerId = seriesTimerId,
)

fun BaseItemDto.getResumePositionTicks() = userData?.playbackPositionTicks ?: 0

fun Collection<LegacyBaseItemdto>.mapBaseItemCollection(): List<BaseItemDto> = map { it.asSdk() }
fun Array<LegacyBaseItemdto>.mapBaseItemArray(): List<BaseItemDto> = map { it.asSdk() }

fun MediaSourceInfo.getVideoStream() = mediaStreams?.firstOrNull {
	it.type == org.jellyfin.sdk.model.api.MediaStreamType.VIDEO
}

fun MediaSourceInfo.getDefaultAudioStream(): MediaStream? {
	val audioStreams = mediaStreams.orEmpty().filter { it.type == org.jellyfin.sdk.model.api.MediaStreamType.AUDIO }
	return audioStreams.firstOrNull { it.index == defaultAudioStreamIndex }
		?: audioStreams.firstOrNull { it.isDefault }
		?: audioStreams.firstOrNull()
}

object FakeBaseItem {
	val FAV_SONGS_ID = UUID.fromString("11111111-0000-0000-0000-000000000001")
	val FAV_SONGS = BaseItemDto(
		id = FAV_SONGS_ID,
		type = BaseItemKind.PLAYLIST,
		isFolder = true,
	)

	val SERIES_TIMERS_ID = UUID.fromString("11111111-0000-0000-0000-000000000002")
	val SERIES_TIMERS = BaseItemDto(
		id = SERIES_TIMERS_ID,
		type = BaseItemKind.FOLDER,
		collectionType = "SeriesTimers",
	)
}
