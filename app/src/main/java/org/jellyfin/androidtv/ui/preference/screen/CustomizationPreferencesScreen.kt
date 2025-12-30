package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.preference.constant.RatingType
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
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
		}

		category {
			setTitle(R.string.pref_browsing)

			link {
				setTitle(R.string.home_prefs)
				setContent(R.string.pref_home_description)
				icon = R.drawable.ic_house
				withFragment<HomePreferencesScreen>()
			}

			link {
				setTitle(R.string.pref_libraries)
				setContent(R.string.pref_libraries_description)
				icon = R.drawable.ic_grid
				withFragment<LibrariesPreferencesScreen>()
			}

			list {
				setTitle(R.string.pref_next_up_retention_days)

				val daysOptions = NEXT_UP_RETENTION_DAYS_OPTIONS

				entries = buildMap {
					put("0", getString(R.string.pref_next_up_retention_days_unlimited))
					daysOptions.forEach { days ->
						put(days.toString(), context.getQuantityString(R.plurals.days, days, days))
					}
				}

				bind {
					get { userPreferences[UserPreferences.nextUpRetentionDays].toString() }
					set { value -> userPreferences[UserPreferences.nextUpRetentionDays] = value.toInt() }
					default { UserPreferences.nextUpRetentionDays.defaultValue.toString() }
				}
			}
		}
	}

	private companion object {
		private const val RETENTION_ONE_WEEK_DAYS = 7
		private const val RETENTION_TWO_WEEKS_DAYS = 14
		private const val RETENTION_ONE_MONTH_DAYS = 30
		private const val RETENTION_TWO_MONTHS_DAYS = 60
		private const val RETENTION_THREE_MONTHS_DAYS = 90
		private const val RETENTION_SIX_MONTHS_DAYS = 180
		private const val RETENTION_ONE_YEAR_DAYS = 365

		private val NEXT_UP_RETENTION_DAYS_OPTIONS = listOf(
			RETENTION_ONE_WEEK_DAYS,
			RETENTION_TWO_WEEKS_DAYS,
			RETENTION_ONE_MONTH_DAYS,
			RETENTION_TWO_MONTHS_DAYS,
			RETENTION_THREE_MONTHS_DAYS,
			RETENTION_SIX_MONTHS_DAYS,
			RETENTION_ONE_YEAR_DAYS,
		)
	}
}
