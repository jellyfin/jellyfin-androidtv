package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class StillWatchingBehavior(
	override val nameRes: Int
) : PreferenceEnum {
	TEST_EPISODE_COUNT(R.string.lbl_still_watching_test_episode_count),
	TEST_MIN_MINUTES(R.string.lbl_still_watching_test_min_minutes),
	/**
	 * Takes shorter than Netflix implementation to show still watching screen.
	 */
	SHORT(R.string.lbl_still_watching_short),
	/**
	 * Default behavior for still watching. Matches Netflix implementation
 	*/
	DEFAULT(R.string.lbl_still_watching_default),
	/**
	 * Takes longer than Netflix implementation to show still watching screen.
	 */
	LONG(R.string.lbl_still_watching_long),
	/**
	 * Takes longer than Netflix implementation to show still watching screen.
	 */
	VERY_LONG(R.string.lbl_still_watching_very_long),
	/**
	 * Disables still watching screen.
	 */
	DISABLED(R.string.state_disabled)
}
