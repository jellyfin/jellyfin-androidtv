package org.jellyfin.androidtv.constant

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.PreferenceEnum
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions

/**
 * All possible homesections, synced with jellyfin-web
 */
enum class HomeSectionType(
	override val serializedName: String
) : PreferenceEnum {
	@EnumDisplayOptions(R.string.home_section_latest_media)
	LATEST_MEDIA("latestmedia"),

	@EnumDisplayOptions(R.string.home_section_library)
	LIBRARY_TILES_SMALL("smalllibrarytiles"),

	@EnumDisplayOptions(R.string.home_section_library_small)
	LIBRARY_BUTTONS("librarybuttons"),

	@EnumDisplayOptions(R.string.home_section_resume)
	RESUME("resume"),

	@EnumDisplayOptions(R.string.home_section_resume_audio)
	RESUME_AUDIO("resumeaudio"),

	@EnumDisplayOptions(R.string.home_section_resume_book)
	RESUME_BOOK("resumebook"),

	@EnumDisplayOptions(R.string.home_section_active_recordings)
	ACTIVE_RECORDINGS("activerecordings"),

	@EnumDisplayOptions(R.string.home_section_next_up)
	NEXT_UP("nextup"),

	@EnumDisplayOptions(R.string.home_section_rewatching)
	REWATCHING("rewatching"),

	@EnumDisplayOptions(R.string.home_section_livetv)
	LIVE_TV("livetv"),

	@EnumDisplayOptions(R.string.home_section_none)
	NONE("none"),
}
