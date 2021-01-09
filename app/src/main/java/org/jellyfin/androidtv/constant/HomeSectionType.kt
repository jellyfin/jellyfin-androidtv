package org.jellyfin.androidtv.constant

/**
 * All possible homesections, synced with jellyfin-web
 */
enum class HomeSectionType(
	val id: String
) {
	LATEST_MEDIA("latestmedia"),
	LIBRARY_TILES_SMALL("smalllibrarytiles"),
	LIBRARY_BUTTONS("librarybuttons"),
	RESUME("resume"),
	RESUME_AUDIO("resumeaudio"),
	ACTIVE_RECORDINGS("activerecordings"),
	NEXT_UP("nextup"),
	LIVE_TV("livetv"),
	NONE("none");

	companion object {
		fun getById(id: String) = values().firstOrNull { it.id == id }
	}
}
