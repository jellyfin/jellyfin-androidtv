package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.koin.android.ext.android.inject

class CrashReportingPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()

	override val screen by optionsScreen {
		setTitle(R.string.pref_acra_category)

		category {
			checkbox {
				setTitle(R.string.pref_enable_acra)
				setContent(R.string.pref_acra_enabled, R.string.pref_acra_disabled)
				bind(userPreferences, UserPreferences.acraEnabled)
			}

			checkbox {
				setTitle(R.string.pref_acra_alwaysaccept)
				setContent(R.string.pref_acra_alwaysaccept_enabled, R.string.pref_acra_alwaysaccept_disabled)
				bind(userPreferences, UserPreferences.acraNoPrompt)
				depends { userPreferences[UserPreferences.acraEnabled] }
			}

			checkbox {
				setTitle(R.string.pref_acra_syslog)
				setContent(R.string.pref_acra_syslog_enabled, R.string.pref_acra_syslog_disabled)
				bind(userPreferences, UserPreferences.acraIncludeSystemLogs)
				depends { userPreferences[UserPreferences.acraEnabled] }
			}
		}
	}
}
