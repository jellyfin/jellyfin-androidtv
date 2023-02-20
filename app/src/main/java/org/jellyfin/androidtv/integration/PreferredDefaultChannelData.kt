package org.jellyfin.androidtv.integration

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions

enum class PreferredDefaultChannelData {
	/**
	 * My media libraries
	 */
	@EnumDisplayOptions(R.string.lbl_my_media)
	MY_MEDIA,

	/**
	 * Next up
	 */
	@EnumDisplayOptions(R.string.lbl_next_up)
	NEXT_UP,

	/**
	 * Latest media
	 */
	@EnumDisplayOptions(R.string.home_section_latest_media)
	LATEST_MEDIA,

	/**
	 * Latest movies
	 */
	@EnumDisplayOptions(R.string.lbl_movies)
	LATEST_MOVIES,

	/**
	 * Latest episodes
	 */
	@EnumDisplayOptions(R.string.lbl_new_episodes)
	LATEST_EPISODES,

	/**
	 * Resume watching
	 */
	@EnumDisplayOptions(R.string.home_section_resume)
	RESUME_WATCHING
}
