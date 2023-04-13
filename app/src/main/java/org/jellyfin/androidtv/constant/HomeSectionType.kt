package org.jellyfin.androidtv.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

/**
 * All possible homesections, "synced" with jellyfin-web.
 *
 * https://github.com/jellyfin/jellyfin-web/blob/master/src/components/homesections/homesections.js
 */
enum class HomeSectionType(
	override val serializedName: String,
	override val nameRes: Int,
) : PreferenceEnum {
	LATEST_MEDIA("latestmedia", R.string.home_section_latest_media),
	LIBRARY_TILES_SMALL("smalllibrarytiles", R.string.home_section_library),
	LIBRARY_BUTTONS("librarybuttons", R.string.home_section_library_small),
	RESUME("resume", R.string.home_section_resume),
	RESUME_AUDIO("resumeaudio", R.string.home_section_resume_audio),
	RESUME_BOOK("resumebook", R.string.home_section_resume_book),
	ACTIVE_RECORDINGS("activerecordings", R.string.home_section_active_recordings),
	NEXT_UP("nextup", R.string.home_section_next_up),
	LIVE_TV("livetv", R.string.home_section_livetv),
	NONE("none", R.string.home_section_none),
}
