package org.jellyfin.androidtv.ui.preference.screen

import android.os.Build
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.getQualityProfiles
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior
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
				setContent(R.string.sum_tv_queuing)
				bind(userPreferences, UserPreferences.mediaQueuingEnabled)
			}
		}

		category {
			setTitle(R.string.pref_video)

			enum<PreferredVideoPlayer> {
				setTitle(R.string.pref_media_player)
				bind(userPreferences, UserPreferences.videoPlayer)
			}

			@Suppress("MagicNumber")
			list {
				setTitle(R.string.pref_max_bitrate_title)
				entries = getQualityProfiles(context)
				bind(userPreferences, UserPreferences.maxBitrate)
			}

			enum<RefreshRateSwitchingBehavior> {
				setTitle(R.string.lbl_refresh_switching)
				bind(userPreferences, UserPreferences.refreshRateSwitchingBehavior)
				depends { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
			}
		}

		category {
			setTitle(R.string.pref_live_tv_cat)

			enum<PreferredVideoPlayer> {
				setTitle(R.string.pref_media_player)
				bind(userPreferences, UserPreferences.liveTvVideoPlayer)
			}

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
				depends { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
			}

			checkbox {
				setTitle(R.string.lbl_bitstream_dts)
				setContent(R.string.desc_bitstream_ac3)
				bind(userPreferences, UserPreferences.dtsEnabled)
				depends { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
			}

			@Suppress("MagicNumber")
			seekbar {
				setTitle(R.string.pref_libvlc_audio_delay_title)
				min = -5_000
				max = 5_000
				valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
					override fun display(value: Int) = "${value}ms"
				}
				bind(userPreferences, UserPreferences.libVLCAudioDelay)
			}
		}
	}
}
