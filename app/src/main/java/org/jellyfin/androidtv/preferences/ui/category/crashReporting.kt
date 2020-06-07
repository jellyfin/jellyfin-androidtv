package org.jellyfin.androidtv.preferences.ui.category

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.UserPreferences
import org.jellyfin.androidtv.preferences.ui.dsl.OptionsScreen
import org.jellyfin.androidtv.preferences.ui.dsl.checkbox

fun OptionsScreen.crashReportingCategory(
	userPreferences: UserPreferences
) = category {
	setTitle(R.string.pref_acra_category)

	checkbox {
		setTitle(R.string.pref_enable_acra)
//		setContent(R.string.pref_acra_enabled, R.string.pref_acra_disabled)
		bind(userPreferences, UserPreferences.acraEnabled)
	}

	checkbox {
		setTitle(R.string.pref_acra_alwaysaccept)
//		setContent(R.string.pref_acra_alwaysaccept_enabled, R.string.pref_acra_alwaysaccept_disabled)
		bind(userPreferences, UserPreferences.acraNoPrompt)
		depends { userPreferences[UserPreferences.acraEnabled] }
	}

	checkbox {
		setTitle(R.string.pref_acra_syslog)
//		setContent(R.string.pref_acra_syslog_enabled, R.string.pref_acra_syslog_disabled)
		bind(userPreferences, UserPreferences.acraIncludeSystemLogs)
		depends { userPreferences[UserPreferences.acraEnabled] }
	}
}
