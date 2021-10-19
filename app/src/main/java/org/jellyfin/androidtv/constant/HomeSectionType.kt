package org.jellyfin.androidtv.constant

import org.jellyfin.androidtv.preference.PreferenceEnum

/**
 * All possible homesections, synced with jellyfin-web
 */
enum class HomeSectionType(
	override val serializedName: String
) : PreferenceEnum {
	LATEST_MEDIA("latestmedia"),
	LIBRARY_TILES_SMALL("smalllibrarytiles"),
	LIBRARY_BUTTONS("librarybuttons"),
	RESUME("resume"),
	RESUME_AUDIO("resumeaudio"),
	ACTIVE_RECORDINGS("activerecordings"),
	NEXT_UP("nextup"),
	LIVE_TV("livetv"),
	NONE("none"),
}
