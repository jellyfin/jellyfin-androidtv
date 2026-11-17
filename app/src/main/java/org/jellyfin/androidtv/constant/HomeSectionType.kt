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
	// Unknown to the server, so home sections are stored locally only
	RANDOM_ITEMS("randomitems", R.string.home_section_random_items),
	HIDDEN_GEMS("hiddengems", R.string.home_section_hidden_gems),
	RECENTLY_ADDED_ALL("recentlyaddedall", R.string.home_section_recently_added_all),
	LATEST_RELEASES("latestreleases", R.string.home_section_latest_releases),
	BECAUSE_YOU_WATCHED("becauseyouwatched", R.string.home_section_because_you_watched),
	STARRING("starring", R.string.home_section_starring),
	DIRECTED_BY("directedby", R.string.home_section_directed_by),
	NONE("none", R.string.home_section_none),
}
