package org.jellyfin.androidtv.preferences.enums

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.ui.dsl.EnumDisplayOptions

enum class LoginBehavior {
	/**
	 * Show login screen when starting the app
	 */
	@EnumDisplayOptions(R.string.pref_show_login)
	SHOW_LOGIN,

	/**
	 * Login as the user who set this setting
	 */
	@EnumDisplayOptions(R.string.pref_auto_login)
	AUTO_LOGIN
}
