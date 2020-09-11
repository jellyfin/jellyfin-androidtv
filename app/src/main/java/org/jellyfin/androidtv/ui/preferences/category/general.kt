package org.jellyfin.androidtv.ui.preferences.category

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.UserPreferences
import org.jellyfin.androidtv.preferences.enums.AppTheme
import org.jellyfin.androidtv.preferences.enums.GridDirection
import org.jellyfin.androidtv.ui.preferences.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preferences.dsl.checkbox
import org.jellyfin.androidtv.ui.preferences.dsl.enum

fun OptionsScreen.generalCategory(
	userPreferences: UserPreferences
) = category {
	setTitle(R.string.pref_general)

	enum<AppTheme> {
		setTitle(R.string.pref_app_theme)
		bind(userPreferences, UserPreferences.appTheme)
	}

	checkbox {
		setTitle(R.string.lbl_show_backdrop)
		bind(userPreferences, UserPreferences.backdropEnabled)
	}

	checkbox {
		setTitle(R.string.lbl_show_premieres)
		setContent(R.string.desc_premieres)
		bind(userPreferences, UserPreferences.premieresEnabled)
	}

	enum<GridDirection> {
		setTitle(R.string.grid_direction)
		bind(userPreferences, UserPreferences.gridDirection)
	}

	checkbox {
		setTitle(R.string.lbl_enable_seasonal_themes)
		setContent(R.string.desc_seasonal_themes)
		bind(userPreferences, UserPreferences.seasonalGreetingsEnabled)
	}

	checkbox {
		setTitle(R.string.lbl_enable_debug)
		setContent(R.string.desc_debug)
		bind(userPreferences, UserPreferences.debuggingEnabled)
	}
}
