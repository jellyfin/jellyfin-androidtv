package org.jellyfin.androidtv.ui.preference.category

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.preference.constant.GridDirection
import org.jellyfin.androidtv.preference.constant.RatingType
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.enum

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

	enum<RatingType> {
		setTitle(R.string.pref_default_rating)
		bind(userPreferences, UserPreferences.defaultRatingType)
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
