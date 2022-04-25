package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions

enum class RefreshRateSwitchingBehavior {
	@EnumDisplayOptions(R.string.lbl_disabled)
	DISABLED,

	/**
	 * When comparing modes, use difference in resolution to rank modes.
	 */
	@EnumDisplayOptions(R.string.pref_refresh_rate_scale_on_tv)
	SCALE_ON_TV,

	/**
	 *  When comparing modes, rank native resolution modes highest.
	 *  Otherwise use difference in resolution to rank modes.
	 */
	@EnumDisplayOptions(R.string.pref_refresh_rate_scale_on_device)
	SCALE_ON_DEVICE,
}
