package org.jellyfin.androidtv.ui.preference.category

import android.app.Activity
import android.app.AlertDialog
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer
import org.jellyfin.androidtv.ui.preference.custom.DurationSeekBarPreference
import org.jellyfin.androidtv.ui.preference.dsl.*
import org.jellyfin.androidtv.util.DeviceUtils
import org.jellyfin.androidtv.util.TimeUtils

fun OptionsScreen.playbackCategory(
	activity: Activity,
	userPreferences: UserPreferences
) = category {
	setTitle(R.string.pref_playback)

	list {
		setTitle(R.string.pref_max_bitrate_title)
		entries = setOf(
			0.0, // auto
			120.0, 110.0, 100.0, // 100 >=
			90.0, 80.0, 70.0, 60.0, 50.0, 40.0, 30.0, 21.0, 15.0, 10.0, // 10 >=
			5.0, 3.0, 2.0, 1.5, 1.0, // 1 >=
			0.72, 0.42 // 0 >=
		).map {
			val value = when {
				it == 0.0 -> activity.getString(R.string.bitrate_auto)
				it >= 1.0 -> activity.getString(R.string.bitrate_mbit, it)
				else -> activity.getString(R.string.bitrate_kbit, it * 100.0)
			}

			it.toString().removeSuffix(".0") to value
		}.toMap()
		bind(userPreferences, UserPreferences.maxBitrate)
	}

	checkbox {
		setTitle(R.string.lbl_tv_queuing)
		setContent(R.string.sum_tv_queuing)
		bind(userPreferences, UserPreferences.mediaQueuingEnabled)
	}

	enum<NextUpBehavior> {
		setTitle(R.string.pref_next_up_behavior_title)
		bind(userPreferences, UserPreferences.nextUpBehavior)
		depends { userPreferences[UserPreferences.mediaQueuingEnabled] }
	}

	seekbar {
		setTitle(R.string.pref_next_up_timeout_title)
		setContent(R.string.pref_next_up_timeout_summary)
		min = 0
		max = 30000
		increment = 1000
		valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
			override fun display(value: Int) = if (value == 0) "∞" else "${value / 1000}s"
		}
		bind(userPreferences, UserPreferences.nextUpTimeout)
		depends { userPreferences[UserPreferences.mediaQueuingEnabled]
			&& userPreferences[UserPreferences.nextUpBehavior] != NextUpBehavior.DISABLED }
	}

	list {
		setTitle(R.string.lbl_resume_preroll)
		entries = setOf(
			0, // Disable
			3, 5, 7, // 10<
			10, 20, 30, 60, // 100<
			120, 300
		).map {
			val value = if (it == 0) activity.getString(R.string.lbl_none)
			else TimeUtils.formatSeconds(context, it)

			it.toString() to value
		}.toMap()
		bind(userPreferences, UserPreferences.resumeSubtractDuration)
	}

	enum<PreferredVideoPlayer> {
		setTitle(R.string.pref_media_player)
		bind(userPreferences, UserPreferences.videoPlayer)
	}

	checkbox {
		setTitle(R.string.lbl_enable_cinema_mode)
		setContent(R.string.sum_enable_cinema_mode)
		bind(userPreferences, UserPreferences.cinemaModeEnabled)
		depends { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
	}

	enum<AudioBehavior> {
		setTitle(R.string.lbl_audio_output)
		bind(userPreferences, UserPreferences.audioBehaviour)
		depends { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
	}

	checkbox {
		setTitle(R.string.lbl_bitstream_ac3)
		setContent(R.string.desc_bitstream_ac3)
		bind(userPreferences, UserPreferences.ac3Enabled)
		depends { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL && !DeviceUtils.is60() }
	}

	checkbox {
		setTitle(R.string.lbl_bitstream_dts)
		setContent(R.string.desc_bitstream_ac3)
		bind(userPreferences, UserPreferences.dtsEnabled)
		depends { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
	}

	checkbox {
		setTitle(R.string.lbl_refresh_switching)
		bind(userPreferences, UserPreferences.refreshRateSwitchingEnabled)
		depends { DeviceUtils.is60() && userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
	}

	seekbar {
		setTitle(R.string.pref_libvlc_audio_delay_title)
		min = -5000
		max = 5000
		valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
			override fun display(value: Int) = "${value}ms"
		}
		bind(userPreferences, UserPreferences.libVLCAudioDelay)
	}

	checkbox {
		setTitle(R.string.pref_use_direct_path_title)
		setContent(R.string.pref_use_direct_path_summary)
		bind {
			get { userPreferences[UserPreferences.externalVideoPlayerSendPath] }
			set {
				if (it) {
					AlertDialog.Builder(activity)
						.setTitle(activity.getString(R.string.lbl_warning))
						.setMessage(activity.getString(R.string.msg_external_path))
						.setPositiveButton(R.string.btn_got_it, null)
						.show()
				}

				userPreferences[UserPreferences.externalVideoPlayerSendPath] = it
			}
			default { userPreferences.getDefaultValue(UserPreferences.externalVideoPlayerSendPath) }
		}
		depends { userPreferences[UserPreferences.videoPlayer] == PreferredVideoPlayer.EXTERNAL }
	}
}
