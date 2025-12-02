package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

enum class StillWatchingBehavior(
	override val nameRes: Int,
	val enabled: Boolean,
	val episodeCount: Int,
	val minDuration: Duration,
) : PreferenceEnum {
	/**
	 * Shorter than default. Show screen at 2 episodes or 60 minutes of uninterrupted watch time.
	 */
	SHORT(
		nameRes = R.string.lbl_still_watching_short,
		enabled = true,
		episodeCount = 2,
		minDuration = 1.hours,
	),

	/**
	 * Default behavior for still watching. Show screen at 3 episodes or 90 minutes of uninterrupted watch time.
	 */
	DEFAULT(
		nameRes = R.string.lbl_still_watching_default,
		enabled = true,
		episodeCount = 3,
		minDuration = 1.5.hours,
	),

	/**
	 * Longer than default. Show screen at 5 episodes or 150 minutes of uninterrupted watch time.
	 */
	LONG(
		nameRes = R.string.lbl_still_watching_long,
		enabled = true,
		episodeCount = 5,
		minDuration = 2.5.hours,
	),

	/**
	 * Much longer than default. Show screen at 8 episodes or 240 minutes of uninterrupted watch time.
	 */
	VERY_LONG(
		nameRes = R.string.lbl_still_watching_very_long,
		enabled = true,
		episodeCount = 8,
		minDuration = 4.hours,
	),

	/**
	 * Disables still watching screen.
	 */
	DISABLED(
		nameRes = R.string.state_disabled,
		enabled = false,
		episodeCount = 0,
		minDuration = Duration.ZERO,
	)
}
