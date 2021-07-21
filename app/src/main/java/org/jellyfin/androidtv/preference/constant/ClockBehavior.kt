package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions

enum class ClockBehavior {
	/**
	 * Always show clock.
	 */
	@EnumDisplayOptions(R.string.lbl_always)
	ALWAYS,

	/**
	 * Show clock in menus only.
	 */
	@EnumDisplayOptions(R.string.pref_clock_display_browsing)
	IN_MENUS,

	/**
	 * Show clock in video only.
	 */
	@EnumDisplayOptions(R.string.pref_clock_display_playback)
	IN_VIDEO,

	/**
	 * Show clock never.
	 */
	@EnumDisplayOptions(R.string.lbl_never)
	NEVER
}
