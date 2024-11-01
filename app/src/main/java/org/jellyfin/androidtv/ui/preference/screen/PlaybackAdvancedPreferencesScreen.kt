package org.jellyfin.androidtv.ui.preference.screen

import android.os.Build
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.getQualityProfiles
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior
import org.jellyfin.androidtv.preference.constant.ZoomMode
import org.jellyfin.androidtv.ui.preference.custom.DurationSeekBarPreference
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.ui.preference.dsl.list
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.seekbar
import org.jellyfin.androidtv.util.TimeUtils
import org.koin.android.ext.android.inject

class PlaybackAdvancedPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()

	override val screen by optionsScreen {
		setTitle(R.string.pref_playback)

		category {
			setTitle(R.string.pref_customization)

			@Suppress("MagicNumber")
			list {
				setTitle(R.string.lbl_resume_preroll)
				entries = setOf(
					0, // Disable
					3, 5, 7, // 10<
					10, 20, 30, 60, // 100<
					120, 300
				).associate {
					val value = if (it == 0) getString(R.string.lbl_none)
					else TimeUtils.formatSeconds(context, it)

					it.toString() to value
				}
				bind(userPreferences, UserPreferences.resumeSubtractDuration)
			}

			checkbox {
				setTitle(R.string.lbl_tv_queuing)
				bind(userPreferences, UserPreferences.mediaQueuingEnabled)
			}

			@Suppress("MagicNumber")
			seekbar {
				setTitle(R.string.lbl_playback_ui_fade_time)
				min = 1
				max = 10
				increment = 1
				valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
					override fun display(value: Int): String = "${value}s"
				}
				bind(userPreferences, UserPreferences.playbackUiFadeTime)
			}
		}

		category {
			setTitle(R.string.pref_video)

			@Suppress("MagicNumber")
			list {
				setTitle(R.string.pref_max_bitrate_title)
				entries = getQualityProfiles(context)
				bind(userPreferences, UserPreferences.maxBitrate)
			}

			enum<RefreshRateSwitchingBehavior> {
				setTitle(R.string.lbl_refresh_switching)
				bind(userPreferences, UserPreferences.refreshRateSwitchingBehavior)
			}

			@Suppress("MagicNumber")
			seekbar {
				setTitle(R.string.video_start_delay)
				min = 0
				max = 5_000
				increment = 250
				valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
					override fun display(value: Int): String = "${value.toDouble() / 1000}s"
				}
				bind {
					get { userPreferences[UserPreferences.videoStartDelay].toInt() }
					set { value -> userPreferences[UserPreferences.videoStartDelay] = value.toLong() }
					default { UserPreferences.videoStartDelay.defaultValue.toInt() }
				}
			}

			enum<ZoomMode> {
				setTitle(R.string.default_video_zoom)
				bind(userPreferences, UserPreferences.playerZoomMode)
			}

			checkbox{
				setTitle(R.string.pref_external_player)
				bind(userPreferences, UserPreferences.useExternalPlayer)
			}
		}

		category {
			setTitle(R.string.pref_live_tv_cat)

			checkbox {
				setTitle(R.string.lbl_direct_stream_live)
				bind(userPreferences, UserPreferences.liveTvDirectPlayEnabled)
				setContent(R.string.pref_direct_stream_live_on, R.string.pref_direct_stream_live_off)
			}
		}

		category {
			setTitle(R.string.pref_audio)

			checkbox {
				setTitle(R.string.pref_audio_night_mode)
				setContent(R.string.desc_audio_night_mode)
				bind(userPreferences, UserPreferences.audioNightMode)
				depends { Build.VERSION.SDK_INT >= Build.VERSION_CODES.P }
			}

			checkbox {
				setTitle(R.string.lbl_bitstream_ac3)
				setContent(R.string.desc_bitstream_ac3)
				bind(userPreferences, UserPreferences.ac3Enabled)
			}
		}
	}
}
