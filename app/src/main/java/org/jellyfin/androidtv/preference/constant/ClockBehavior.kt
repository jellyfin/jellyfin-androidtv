package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class ClockBehavior(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 * Always show clock.
	 */
	ALWAYS(R.string.lbl_always),

	/**
	 * Show clock in menus only.
	 */
	IN_MENUS(R.string.pref_clock_display_browsing),

	/**
	 * Show clock in video only.
	 */
	IN_VIDEO(R.string.pref_clock_display_playback),

	/**
	 * Show clock never.
	 */
	NEVER(R.string.lbl_never),
}
