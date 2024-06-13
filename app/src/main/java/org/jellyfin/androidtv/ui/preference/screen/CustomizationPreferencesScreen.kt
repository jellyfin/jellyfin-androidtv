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
import org.jellyfin.androidtv.ui.preference.dsl.list
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.shortcut
import org.jellyfin.androidtv.util.getQuantityString
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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

			checkbox {
				setTitle(R.string.lbl_show_premieres)
				setContent(R.string.desc_premieres)
				bind(userPreferences, UserPreferences.premieresEnabled)
			}

			checkbox {
				setTitle(R.string.pref_enable_media_management)
				setContent(R.string.pref_enable_media_management_description)
				bind(userPreferences, UserPreferences.mediaManagementEnabled)
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
		}

		category {
			setTitle(R.string.pref_screensaver)

			checkbox {
				setTitle(R.string.pref_screensaver_inapp_enabled)
				setContent(R.string.pref_screensaver_inapp_enabled_description)
				bind(userPreferences, UserPreferences.screensaverInAppEnabled)
			}

			@Suppress("MagicNumber")
			list {
				setTitle(R.string.pref_screensaver_inapp_timeout)

				entries = mapOf(
					30.seconds to context.getQuantityString(R.plurals.seconds, 30),
					1.minutes to context.getQuantityString(R.plurals.minutes, 1),
					2.5.minutes to context.getQuantityString(R.plurals.minutes, 2.5),
					5.minutes to context.getQuantityString(R.plurals.minutes, 5),
					10.minutes to context.getQuantityString(R.plurals.minutes, 10),
					15.minutes to context.getQuantityString(R.plurals.minutes, 15),
					30.minutes to context.getQuantityString(R.plurals.minutes, 30),
				).mapKeys { it.key.inWholeMilliseconds.toString() }

				bind {
					get { userPreferences[UserPreferences.screensaverInAppTimeout].toString() }
					set { value -> userPreferences[UserPreferences.screensaverInAppTimeout] = value.toLong() }
					default { UserPreferences.screensaverInAppTimeout.defaultValue.toString() }
				}

				depends { userPreferences[UserPreferences.screensaverInAppEnabled] }
			}

			checkbox {
				setTitle(R.string.pref_screensaver_ageratingrequired_title)
				setContent(
					R.string.pref_screensaver_ageratingrequired_enabled,
					R.string.pref_screensaver_ageratingrequired_disabled,
				)

				bind(userPreferences, UserPreferences.screensaverAgeRatingRequired)
			}

			list {
				setTitle(R.string.pref_screensaver_ageratingmax)

				// Note: Must include 13 (default value)
				// We may want to fetch this mapping from the server in the future
				@Suppress("MagicNumber")
				val ages = setOf(5, 10, 13, 14, 16, 18, 21)

				entries = buildMap {
					put("0", getString(R.string.pref_screensaver_ageratingmax_zero))
					ages.forEach { age -> put(age.toString(), getString(R.string.pref_screensaver_ageratingmax_entry, age)) }
					put("-1", getString(R.string.pref_screensaver_ageratingmax_unlimited))
				}

				bind {
					get { userPreferences[UserPreferences.screensaverAgeRatingMax].toString() }
					set { value -> userPreferences[UserPreferences.screensaverAgeRatingMax] = value.toInt() }
					default { UserPreferences.screensaverAgeRatingMax.defaultValue.toString() }
				}
			}
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
