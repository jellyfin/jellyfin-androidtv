package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class RefreshRateSwitchingBehavior(
	override val nameRes: Int,
) : PreferenceEnum {
	DISABLED(R.string.state_disabled),

	/**
	 * When comparing modes, use difference in resolution to rank modes.
	 */
	SCALE_ON_TV(R.string.pref_refresh_rate_scale_on_tv),

	/**
	 *  When comparing modes, rank native resolution modes highest.
	 *  Otherwise use difference in resolution to rank modes.
	 */
	SCALE_ON_DEVICE(R.string.pref_refresh_rate_scale_on_device),
}
