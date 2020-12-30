package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.preference.category.*
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.koin.android.ext.android.inject

class UserPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()

	override val screen by optionsScreen {
		setTitle(R.string.settings_title)

		// Add all categories using extension functions in the "category" subpackage
		link {
			setTitle(R.string.pref_authentication_link)
			icon = R.drawable.ic_users
			withFragment<AuthPreferencesScreen>()
		}
		generalCategory(userPreferences)
		playbackCategory(requireActivity(), userPreferences)
		liveTvCategory(userPreferences)
		shortcutsCategory(userPreferences)
		crashReportingCategory(userPreferences)
		aboutCategory()
	}
}
