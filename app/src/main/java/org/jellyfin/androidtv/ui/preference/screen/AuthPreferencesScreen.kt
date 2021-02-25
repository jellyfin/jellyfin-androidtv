package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.preference.category.authenticationCategory
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.lazyOptionsScreen
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

class AuthPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()

	override val screen by lazyOptionsScreen {
		setTitle(R.string.pref_authentication_cat)

		authenticationCategory(userPreferences, get())
	}
}
