package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.preference.AuthenticationPreferences
import org.jellyfin.androidtv.ui.preference.category.authenticationCategory
import org.jellyfin.androidtv.ui.preference.category.manageServersCategory
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.lazyOptionsScreen
import org.koin.android.ext.android.inject

class AuthPreferencesScreen : OptionsFragment() {
	private val authenticationRepository: AuthenticationRepository by inject()
	private val authenticationPreferences: AuthenticationPreferences by inject()

	override val screen by lazyOptionsScreen {
		setTitle(R.string.pref_authentication_cat)

		authenticationCategory(authenticationRepository, authenticationPreferences)
		manageServersCategory(authenticationRepository)
	}
}
