package org.jellyfin.androidtv.preferences.ui.category

import androidx.preference.PreferenceScreen
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.UserPreferences
import org.jellyfin.androidtv.preferences.enums.AppTheme
import org.jellyfin.androidtv.preferences.enums.GridDirection
import org.jellyfin.androidtv.preferences.ui.dsl.*

fun PreferenceScreen.generalCategory(
	userPreferences: UserPreferences
) = category(R.string.pref_general) {
	enumPreference<AppTheme>(R.string.pref_app_theme) {
		bindEnum(userPreferences, UserPreferences.appTheme)
	}
	checkboxPreference(R.string.lbl_show_backdrop) {
		bind(userPreferences, UserPreferences.backdropEnabled)
	}
	checkboxPreference(R.string.lbl_show_premieres, R.string.desc_premieres) {
		bind(userPreferences, UserPreferences.premieresEnabled)
	}
	enumPreference<GridDirection>(R.string.grid_direction) {
		bindEnum(userPreferences, UserPreferences.gridDirection)
	}
	checkboxPreference(R.string.lbl_enable_seasonal_themes, R.string.desc_seasonal_themes) {
		bind(userPreferences, UserPreferences.seasonalGreetingsEnabled)
	}
	checkboxPreference(R.string.lbl_enable_debug, R.string.desc_debug) {
		bind(userPreferences, UserPreferences.debuggingEnabled)
	}
}
