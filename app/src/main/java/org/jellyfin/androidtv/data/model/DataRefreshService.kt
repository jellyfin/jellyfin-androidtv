package org.jellyfin.androidtv.data.model

import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

class DataRefreshService {
	var lastDeletedItemId: UUID? = null
	var lastPlayback: Long = 0
	var lastMoviePlayback: Long = 0
	var lastTvPlayback: Long = 0
	var lastMusicPlayback: Long = 0
	var lastLibraryChange: Long = 0
	var lastVideoQueueChange: Long = 0
	var lastFavoriteUpdate: Long = 0
	var lastPlayedItem: BaseItemDto? = null
}
