package org.jellyfin.androidtv.onlinesubtitles

object OnlineSubtitleIndexer {

	private var index = 10000

	fun generateUniqueId(): Int {
		return ++index
	}
}
