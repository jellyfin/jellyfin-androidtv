@file:JvmName("JavaCompat")

package org.jellyfin.androidtv.util.sdk.compat

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.UserItemDataDto
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

fun BaseItemDto.copyWithOverview(
	overview: String?,
) = copy(
	overview = overview,
)

fun BaseItemDto.copyWithParentId(
	parentId: UUID?,
) = copy(
	parentId = parentId,
)

fun BaseItemDto.copyWithUserData(
	userData: UserItemDataDto?,
) = copy(
	userData = userData,
)

fun BaseItemDto.getResumePositionTicks() = userData?.playbackPositionTicks ?: 0

fun Collection<LegacyBaseItemdto>.mapBaseItemCollection(): List<BaseItemDto> = map { it.asSdk() }
fun Array<LegacyBaseItemdto>.mapBaseItemArray(): List<BaseItemDto> = map { it.asSdk() }

fun MediaSourceInfo.getVideoStream() = mediaStreams?.firstOrNull {
	it.type == org.jellyfin.sdk.model.api.MediaStreamType.VIDEO
}

val BaseItemDto.canResume get() = (userData?.playbackPositionTicks ?: 0) > 0
