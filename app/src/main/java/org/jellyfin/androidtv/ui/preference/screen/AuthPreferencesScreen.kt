package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.androidtv.preference.AuthenticationPreferences
import org.jellyfin.androidtv.ui.preference.category.aboutCategory
import org.jellyfin.androidtv.ui.preference.category.authenticationAdvancedCategory
import org.jellyfin.androidtv.ui.preference.category.authenticationCategory
import org.jellyfin.androidtv.ui.preference.category.manageServersCategory
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.koin.android.ext.android.inject

class AuthPreferencesScreen : OptionsFragment() {
	private val authenticationRepository: AuthenticationRepository by inject()
	private val authenticationPreferences: AuthenticationPreferences by inject()
	private val sessionRepository: SessionRepository by inject()


	private val showAbout by lazy {
		requireArguments().getBoolean(ARG_SHOW_ABOUT, false)
	}

	override val rebuildOnResume = true

	override val screen get() = optionsScreen {
		setTitle(R.string.pref_authentication_cat)

		authenticationCategory(authenticationRepository, authenticationPreferences, sessionRepository)
		manageServersCategory(authenticationRepository)
		authenticationAdvancedCategory(authenticationPreferences, sessionRepository)

		if (showAbout) aboutCategory()
	}

	companion object {
		const val ARG_SHOW_ABOUT = "show_about"
	}
}
