package org.jellyfin.androidtv.data.model

class DataRefreshService {
	var lastDeletedItemId: String? = null
	var lastPlayback: Long = 0
	var lastMoviePlayback: Long = 0
	var lastTvPlayback: Long = 0
	var lastMusicPlayback: Long = 0
	var lastLibraryChange: Long = 0
	var lastVideoQueueChange: Long = 0
	var lastFavoriteUpdate: Long = 0
}
