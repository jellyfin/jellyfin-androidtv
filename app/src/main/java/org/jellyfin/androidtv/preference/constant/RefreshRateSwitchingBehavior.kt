package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions

enum class RefreshRateSwitchingBehavior {
	/**
	 *  Don't change
	 */
	@EnumDisplayOptions(R.string.lbl_disabled)
	DISABLED,

	/**
	 *  Don't change
	 */
	@EnumDisplayOptions(R.string.pref_refresh_rate_scale_tv)
	SCALE_ON_TV,

	/**
	 *  Force ExoPlayer
	 */
	@EnumDisplayOptions(R.string.pref_refresh_rate_scale_on_device)
	SCALE_ON_DEVICE,
}
