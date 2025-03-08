package org.jellyfin.androidtv.onlinesubtitles

import org.jellyfin.sdk.model.api.BaseItemDto
import java.io.File
import java.util.UUID

interface OnlineSubtitleSource {

	val type : OnlineSubtitleType

	suspend fun fetchSubtitleList(
		baseItemDto: BaseItemDto,
		tmdbId: Int?,
		tmdbStr: String?,
		imdbId: Int?,
		imdbStr: String?,
		fps: Float?,
		seasonNumber: Int?,
		episodeNumber: Int?,
		setSubtitlesForMedia: (mediaId: UUID, type: OnlineSubtitleType, list: List<OnlineSubtitle>) -> Unit
	)

	suspend fun downloadSubtitle(subtitle: OnlineSubtitle, fileToWrite: File, showInfo: (infoText: String) -> Unit): Result<File>
}
