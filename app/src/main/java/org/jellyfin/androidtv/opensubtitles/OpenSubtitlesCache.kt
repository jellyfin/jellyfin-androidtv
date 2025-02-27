package org.jellyfin.androidtv.opensubtitles

import org.jellyfin.sdk.model.UUID

object OpenSubtitlesCache {

	val cacheMap = HashMap<UUID, List<OS_Subtitle>>()

	fun setSubtitlesForMedia(id: UUID, fpsCompatibleList: List<OS_Subtitle>) {
		cacheMap[id] = fpsCompatibleList
	}

	fun getSubtitlesForMedia(id: UUID) : List<OS_Subtitle>?{
		return cacheMap[id]
	}
}
