package org.jellyfin.androidtv.preferences.ui.category

import androidx.preference.PreferenceScreen
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.UserPreferences
import org.jellyfin.androidtv.preferences.ui.dsl.bind
import org.jellyfin.androidtv.preferences.ui.dsl.category
import org.jellyfin.androidtv.preferences.ui.dsl.checkboxPreference

fun PreferenceScreen.crashReportingCategory(
	userPreferences: UserPreferences
) = category(R.string.pref_acra_category) {
	checkboxPreference(R.string.pref_enable_acra, R.string.pref_acra_enabled, R.string.pref_acra_disabled) {
		bind(userPreferences, UserPreferences.acraEnabled)
	}
	checkboxPreference(R.string.pref_acra_alwaysaccept, R.string.pref_acra_alwaysaccept_enabled, R.string.pref_acra_alwaysaccept_disabled) {
		bind(userPreferences, UserPreferences.acraNoPrompt)
	}
	checkboxPreference(R.string.pref_acra_syslog, R.string.pref_acra_syslog_enabled, R.string.pref_acra_syslog_disabled) {
		bind(userPreferences, UserPreferences.acraIncludeSystemLogs)
	}
}
