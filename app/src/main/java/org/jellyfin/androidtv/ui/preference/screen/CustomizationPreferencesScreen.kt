package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.preference.constant.RatingType
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior
import org.jellyfin.androidtv.ui.preference.dsl.*
import org.koin.android.ext.android.inject

class CustomizationPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()

	override val screen by optionsScreen {
		setTitle(R.string.pref_customization)

		category {
			setTitle(R.string.pref_theme)

			enum<AppTheme> {
				setTitle(R.string.pref_app_theme)
				bind(userPreferences, UserPreferences.appTheme)
			}

			checkbox {
				setTitle(R.string.lbl_enable_seasonal_themes)
				setContent(R.string.desc_seasonal_themes)
				bind(userPreferences, UserPreferences.seasonalGreetingsEnabled)
			}

			enum<ClockBehavior> {
				setTitle(R.string.pref_clock_display)
				bind(userPreferences, UserPreferences.clockBehavior)
			}

			enum<WatchedIndicatorBehavior> {
				setTitle(R.string.pref_watched_indicator)
				bind(userPreferences, UserPreferences.watchedIndicatorBehavior)
			}

			checkbox {
				setTitle(R.string.lbl_show_backdrop)
				setContent(R.string.pref_show_backdrop_description)
				bind(userPreferences, UserPreferences.backdropEnabled)
			}

			checkbox {
				setTitle(R.string.lbl_use_series_thumbnails)
				setContent(R.string.lbl_use_series_thumbnails_description)
				bind(userPreferences, UserPreferences.seriesThumbnailsEnabled)
			}

			enum<RatingType> {
				setTitle(R.string.pref_default_rating)
				bind(userPreferences, UserPreferences.defaultRatingType)
			}

			checkbox {
				setTitle(R.string.lbl_show_premieres)
				setContent(R.string.desc_premieres)
				bind(userPreferences, UserPreferences.premieresEnabled)
			}
		}

		category {
			setTitle(R.string.pref_browsing)

			link {
				setTitle(R.string.home_prefs)
				setContent(R.string.pref_home_description)
				icon = R.drawable.ic_house
				withFragment<HomePreferencesScreen>()
			}

			// TODO implement
			// link {
			// 	setTitle(R.string.libraries)
			// 	setContent(R.string.libraries_description)
			// 	icon = R.drawable.ic_grid
			// 	withFragment<HomePreferencesScreen>()
			// }
		}

		category {
			setTitle(R.string.pref_behavior)

			shortcut {
				setTitle(R.string.pref_audio_track_button)
				bind(userPreferences, UserPreferences.shortcutAudioTrack)
			}

			shortcut {
				setTitle(R.string.pref_subtitle_track_button)
				bind(userPreferences, UserPreferences.shortcutSubtitleTrack)
			}
		}
	}
}
