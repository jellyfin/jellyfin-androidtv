package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.category.aboutCategory
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen

class UserPreferencesScreen : OptionsFragment() {
	override val screen by optionsScreen {
		setTitle(R.string.settings_title)

		category {
			link {
				setTitle(R.string.pref_login)
				setContent(R.string.pref_login_description)
				icon = R.drawable.ic_users
				withFragment<AuthPreferencesScreen>()
			}

			link {
				setTitle(R.string.pref_customization)
				setContent(R.string.pref_customization_description)
				icon = R.drawable.ic_adjust
				withFragment<CustomizationPreferencesScreen>()
			}

			link {
				setTitle(R.string.pref_playback)
				setContent(R.string.pref_playback_description)
				icon = R.drawable.ic_next
				withFragment<PlaybackPreferencesScreen>()
			}

			link {
				setTitle(R.string.pref_telemetry_category)
				setContent(R.string.pref_telemetry_description)
				icon = R.drawable.ic_error
				withFragment<CrashReportingPreferencesScreen>()
			}

			link {
				setTitle(R.string.pref_developer_link)
				setContent(R.string.pref_developer_link_description)
				icon = R.drawable.ic_flask
				withFragment<DeveloperPreferencesScreen>()
			}
		}

		aboutCategory()
	}
}
