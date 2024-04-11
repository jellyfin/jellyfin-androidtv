package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_DISABLED
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer
import org.jellyfin.androidtv.ui.preference.custom.DurationSeekBarPreference
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.colorList
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.seekbar
import org.koin.android.ext.android.inject

class PlaybackPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()

	override val screen by optionsScreen {
		setTitle(R.string.pref_playback)

		category {
			setTitle(R.string.pref_customization)

			enum<NextUpBehavior> {
				setTitle(R.string.pref_next_up_behavior_title)
				bind(userPreferences, UserPreferences.nextUpBehavior)
				depends { userPreferences[UserPreferences.mediaQueuingEnabled] }
			}

			@Suppress("MagicNumber")
			seekbar {
				setTitle(R.string.pref_next_up_timeout_title)
				setContent(R.string.pref_next_up_timeout_summary)
				min = 0 // value of 0 disables timer
				max = 30_000
				increment = 1_000
				valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
					override fun display(value: Int): String = when (value) {
						NEXTUP_TIMER_DISABLED -> getString(R.string.pref_next_up_timeout_disabled)
						else -> "${value / 1000}s"
					}
				}
				bind(userPreferences, UserPreferences.nextUpTimeout)
				depends {
					userPreferences[UserPreferences.mediaQueuingEnabled]
						&& userPreferences[UserPreferences.nextUpBehavior] != NextUpBehavior.DISABLED
				}
			}

			checkbox {
				setTitle(R.string.lbl_enable_cinema_mode)
				setContent(R.string.sum_enable_cinema_mode)
				bind(userPreferences, UserPreferences.cinemaModeEnabled)
				depends { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
			}
		}

		category {
			setTitle(R.string.pref_audio)

			enum<AudioBehavior> {
				setTitle(R.string.lbl_audio_output)
				bind(userPreferences, UserPreferences.audioBehaviour)
				depends { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
			}
		}

		category {
			setTitle(R.string.pref_subtitles)

			checkbox {
				setTitle(R.string.pref_subtitles_background_title)
				setContent(R.string.pref_subtitles_background_summary)
				bind(userPreferences, UserPreferences.subtitlesBackgroundEnabled)
			}

			@Suppress("MagicNumber")
			colorList {
				setTitle(R.string.lbl_subtitle_fg)
				entries = mapOf(
					0xFFFFFFFF to context.getString(R.string.color_white),
					0XFF000000 to context.getString(R.string.color_black),
					0xFF7F7F7F to context.getString(R.string.color_darkgrey),
					0xFFC80000 to context.getString(R.string.color_red),
					0xFF00C800 to context.getString(R.string.color_green),
					0xFF0000C8 to context.getString(R.string.color_blue),
					0xFFEEDC00 to context.getString(R.string.color_yellow),
					0xFFD60080 to context.getString(R.string.color_pink),
					0xFF009FDA to context.getString(R.string.color_cyan),
				)
				bind(userPreferences, UserPreferences.subtitlesTextColor)
			}

			@Suppress("MagicNumber")
			seekbar {
				setTitle(R.string.lbl_subtitle_stroke)
				min = 0
				max = 24
				increment = 2
				bind(userPreferences, UserPreferences.subtitleStrokeSize)
			}

			@Suppress("MagicNumber")
			seekbar {
				setTitle(R.string.lbl_subtitle_position)
				min = 0
				max = 300
				increment = 10
				bind(userPreferences, UserPreferences.subtitlePosition)
			}

			@Suppress("MagicNumber")
			seekbar {
				setTitle(R.string.pref_subtitles_size)
				min = 10
				max = 38
				bind(userPreferences, UserPreferences.subtitlesSize)
			}
		}

		category {
			setTitle(R.string.advanced_settings)

			link {
				setTitle(R.string.pref_playback_advanced)
				icon = R.drawable.ic_more
				withFragment<PlaybackAdvancedPreferencesScreen>()
			}
		}
	}
}
