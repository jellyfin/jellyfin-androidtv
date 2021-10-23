package org.jellyfin.androidtv.ui.preference.category

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.preference.constant.RatingType
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior
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

	enum<ClockBehavior> {
		setTitle(R.string.pref_clock_display)
		bind(userPreferences, UserPreferences.clockBehavior)
	}

	enum<RatingType> {
		setTitle(R.string.pref_default_rating)
		bind(userPreferences, UserPreferences.defaultRatingType)
	}

	enum<WatchedIndicatorBehavior> {
		setTitle(R.string.pref_watched_indicator)
		bind(userPreferences, UserPreferences.watchedIndicatorBehavior)
	}

	checkbox {
		setTitle(R.string.lbl_use_series_thumbnails)
		setContent(R.string.lbl_use_series_thumbnails_description)
		bind(userPreferences, UserPreferences.seriesThumbnailsEnabled)
	}

	checkbox {
		setTitle(R.string.enable_home_header)
		setContent(R.string.enable_home_header_description)
		bind(userPreferences, UserPreferences.homeHeaderEnabled)
	}

	checkbox {
		setTitle(R.string.enable_home_thumbnails)
		setContent(R.string.enable_home_thumbnails_description)
		bind(userPreferences, UserPreferences.homeThumbnailsEnabled)
	}

	checkbox {
		setTitle(R.string.lbl_enable_seasonal_themes)
		setContent(R.string.desc_seasonal_themes)
		bind(userPreferences, UserPreferences.seasonalGreetingsEnabled)
	}
}
