package org.jellyfin.androidtv.preferences.ui.category

import android.app.Activity
import android.app.AlertDialog
import androidx.preference.PreferenceScreen
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.UserPreferences
import org.jellyfin.androidtv.preferences.enums.AudioBehavior
import org.jellyfin.androidtv.preferences.enums.PreferredVideoPlayer
import org.jellyfin.androidtv.preferences.ui.dsl.*
import org.jellyfin.androidtv.util.DeviceUtils
import org.jellyfin.androidtv.util.TimeUtils

fun PreferenceScreen.playbackCategory(
	activity: Activity,
	userPreferences: UserPreferences
) = category(R.string.pref_playback) {
	// All values are in megabits
	val maxBitrateValues = setOf(
		0.0, // auto
		120.0, 110.0, 100.0, // 100 >=
		90.0, 80.0, 70.0, 60.0, 50.0, 40.0, 30.0, 21.0, 15.0, 10.0, // 10 >=
		5.0, 3.0, 2.0, 1.5, 1.0, // 1 >=
		0.72, 0.42 // 0 >=
	).map {
		val value = if (it == 0.0) activity.getString(R.string.bitrate_auto)
		else if (it >= 1.0) activity.getString(R.string.bitrate_mbit, it)
		else activity.getString(R.string.bitrate_kbit, it * 100.0)

		it.toString().removeSuffix(".0") to value
	}.toMap()

	// All values are in seconds
	val prerollValues = setOf(
		0, // Disable
		3, 5, 7, // 10<
		10, 20, 30, 60, // 100<
		120, 300
	).map {
		val value = if (it == 0) activity.getString(R.string.lbl_none)
		else TimeUtils.formatSeconds(it)

		it.toString() to value
	}.toMap()

	listPreference(R.string.pref_max_bitrate_title, maxBitrateValues) {
		bind(userPreferences, UserPreferences.maxBitrate)
	}
	checkboxPreference(R.string.lbl_tv_queuing, R.string.sum_tv_queuing) {
		bind(userPreferences, UserPreferences.mediaQueuingEnabled)
	}
	checkboxPreference(R.string.pref_next_up_enabled_title, R.string.pref_next_up_enabled_summary) {
		bind(userPreferences, UserPreferences.nextUpEnabled)
		enabled { userPreferences[UserPreferences.mediaQueuingEnabled] }
	}
	seekbarPreference(R.string.pref_next_up_timeout_title, R.string.pref_next_up_timeout_summary, min = 3000, max = 30000, increment = 1000) {
		bind(userPreferences, UserPreferences.nextUpTimeout)
		enabled { userPreferences[UserPreferences.mediaQueuingEnabled] && userPreferences[UserPreferences.nextUpEnabled] }
	}
	listPreference(R.string.lbl_resume_preroll, prerollValues) {
		bind(userPreferences, UserPreferences.resumeSubtractDuration)
	}
	enumPreference<PreferredVideoPlayer>(R.string.pref_media_player) {
		bindEnum(userPreferences, UserPreferences.videoPlayer)
	}
	checkboxPreference(R.string.lbl_enable_cinema_mode, R.string.sum_enable_cinema_mode) {
		bind(userPreferences, UserPreferences.cinemaModeEnabled)
		enabled { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
	}
	enumPreference<AudioBehavior>(R.string.lbl_audio_output) {
		bindEnum(userPreferences, UserPreferences.audioBehaviour)
		visible { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL && !DeviceUtils.isFireTv() && DeviceUtils.is50() }
	}
	checkboxPreference(R.string.lbl_bitstream_ac3, R.string.desc_bitstream_ac3) {
		bind(userPreferences, UserPreferences.ac3Enabled)
		visible { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL && !DeviceUtils.is60() }
	}
	checkboxPreference(R.string.lbl_bitstream_dts, R.string.desc_bitstream_ac3) {
		bind(userPreferences, UserPreferences.dtsEnabled)
		enabled { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
	}
	checkboxPreference(R.string.lbl_refresh_switching) {
		bind(userPreferences, UserPreferences.refreshRateSwitchingEnabled)
		visible { DeviceUtils.is60() }
		enabled { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
	}
	//TODO Add summary
	//TODO Set inputType to number only
	longPreference(R.string.pref_libvlc_audio_delay_title) {
		bind(userPreferences, UserPreferences.libVLCAudioDelay)
	}
	checkboxPreference(R.string.pref_use_direct_path_title, R.string.pref_use_direct_path_summary) {
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
		enabled { userPreferences[UserPreferences.videoPlayer] == PreferredVideoPlayer.EXTERNAL }
	}
}
