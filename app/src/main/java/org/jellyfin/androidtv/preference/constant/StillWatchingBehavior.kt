package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class StillWatchingBehavior(
	override val nameRes: Int
) : PreferenceEnum {
	/**
	 * Shorter than default. Show screen at 2 episodes or 60 minutes of uninterrupted watch time.
	 */
	SHORT(R.string.lbl_still_watching_short),
	/**
	 * Default behavior for still watching. Show screen at 3 episodes or 90 minutes of uninterrupted watch time.
 	*/
	DEFAULT(R.string.lbl_still_watching_default),
	/**
	 * Longer than default. Show screen at 5 episodes or 150 minutes of uninterrupted watch time.
	 */
	LONG(R.string.lbl_still_watching_long),
	/**
	 * Much longer than default. Show screen at 8 episodes or 240 minutes of uninterrupted watch time.
	 */
	VERY_LONG(R.string.lbl_still_watching_very_long),
	/**
	 * Disables still watching screen.
	 */
	DISABLED(R.string.state_disabled)
}
