package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions

enum class AppTheme {
	/**
	 * The default dark theme
	 */
	@EnumDisplayOptions(R.string.pref_theme_dark)
	DARK,

	/**
	 * The "classic" emerald theme
	 */
	@EnumDisplayOptions(R.string.pref_theme_emerald)
	EMERALD,

	/**
	 * Theme inspired by Win 3.1's "hot dog stand"
	 */
	@EnumDisplayOptions(hidden = true)
	HOT_DOG_STAND
}
