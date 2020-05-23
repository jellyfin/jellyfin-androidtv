package org.jellyfin.androidtv.preferences.enums

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.ui.dsl.EnumDisplayOptions

enum class AppTheme {
	/**
	 * The default dark theme
	 */
	@EnumDisplayOptions(R.string.pref_theme_dark)
	Theme_Jellyfin,

	/**
	 * The "classic" emerald theme
	 */
	@EnumDisplayOptions(R.string.pref_theme_emerald)
	Theme_Jellyfin_Emerald,

	/**
	 * Theme inspired by Win 3.1's "hot dog stand"
	 */
	@EnumDisplayOptions(hidden = true)
	Theme_Jellyfin_HotDogStand
}
