package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.preference.category.*
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.lazyOptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.koin.android.ext.android.inject

class UserPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()

	override val screen by lazyOptionsScreen {
		setTitle(R.string.settings_title)

		category {
			link {
				setTitle(R.string.pref_authentication_link)
				icon = R.drawable.ic_users
				withFragment<AuthPreferencesScreen>()
			}

			link {
				setTitle(R.string.home_prefs)
				icon = R.drawable.ic_house
				withFragment<HomePreferencesScreen>()
			}
		}

		generalCategory(userPreferences)
		playbackCategory(requireActivity(), userPreferences)
		liveTvCategory(userPreferences)
		shortcutsCategory(userPreferences)
		crashReportingCategory(userPreferences)

		link {
			setTitle(R.string.pref_developer_link)
			setContent(R.string.pref_developer_link_description)
			icon = R.drawable.ic_flask
			withFragment<DeveloperPreferencesScreen>()
		}

		aboutCategory()
	}
}
