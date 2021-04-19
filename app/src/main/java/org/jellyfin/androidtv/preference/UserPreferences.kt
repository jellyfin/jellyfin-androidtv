package org.jellyfin.androidtv.preference

import android.content.Context
import android.view.KeyEvent
import androidx.preference.PreferenceManager
import org.acra.ACRA
import org.jellyfin.androidtv.preference.constant.*

/**
 * User preferences are configurable by the user and change behavior of the application.
 * When changing preferences migration should be added to the init function.
 *
 * @param context Context to get the SharedPreferences from
 */
class UserPreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
) {
	companion object {
		/* Display */
		/**
		 * Select the app theme
		 */
		var appTheme = Preference.enum("app_theme", AppTheme.DARK)

		/**
		 * Enable background images while browsing
		 */
		var backdropEnabled = Preference.boolean("pref_show_backdrop", true)

		/**
		 * Show premieres on home screen
		 */
		var premieresEnabled = Preference.boolean("pref_enable_premieres", false)

		/**
		 * Show a little notification to celebrate a set of holidays
		 */
		var seasonalGreetingsEnabled = Preference.boolean("pref_enable_themes", true)

		/**
		 * Show additional debug information
		 */
		var debuggingEnabled = Preference.boolean("pref_enable_debug", false)

		/* Playback - General*/
		/**
		 * Maximum bitrate in megabit for playback. A value of 0 means "auto".
		 */
		var maxBitrate = Preference.string("pref_max_bitrate", "0")

		/**
		 * Auto-play next item
		 */
		var mediaQueuingEnabled = Preference.boolean("pref_enable_tv_queuing", true)

		/**
		 * Enable the next up screen or not
		 */
		var nextUpEnabled = Preference.boolean("next_up_enabled", true)

		/**
		 * Next up timeout before playing next item
		 * Stored in milliseconds
		 */
		var nextUpTimeout = Preference.int("next_up_timeout", 1000 * 7)

		/**
		 * Duration in seconds to subtract from resume time
		 */
		var resumeSubtractDuration = Preference.string("pref_resume_preroll", "0")

		/**
		 * Enable cinema mode
		 */
		var cinemaModeEnabled = Preference.boolean("pref_enable_cinema_mode", true)

		/* Playback - Video */
		/**
		 * Preferred video player.
		 */
		var videoPlayer = Preference.enum("video_player", PreferredVideoPlayer.AUTO)

		/**
		 * Enable refresh rate switching when device supports it
		 */
		var refreshRateSwitchingEnabled = Preference.boolean("pref_refresh_switching", false)

		/**
		 * Send a path instead to the external player
		 */
		var externalVideoPlayerSendPath = Preference.boolean("pref_send_path_external", false)

		/* Playback - Audio related */
		/**
		 * Preferred behavior for audio streaming.
		 */
		var audioBehaviour = Preference.enum("audio_behavior", AudioBehavior.DIRECT_STREAM)

		/**
		 * Enable DTS
		 */
		var dtsEnabled = Preference.boolean("pref_bitstream_dts", false)

		/**
		 * Enable AC3
		 */
		var ac3Enabled = Preference.boolean("pref_bitstream_ac3", true)

		/**
		 * Default audio delay in milliseconds for libVLC
		 */
		var libVLCAudioDelay = Preference.int("libvlc_audio_delay", 0)

		/* Live TV */
		/**
		 * Use direct play
		 */
		var liveTvDirectPlayEnabled = Preference.boolean("pref_live_direct", true)

		/**
		 * Preferred video player for live TV
		 */
		var liveTvVideoPlayer = Preference.enum("live_tv_video_player", PreferredVideoPlayer.AUTO)

		/**
		 * Shortcut used for changing the audio track
		 */
		var shortcutAudioTrack = Preference.int("shortcut_audio_track", KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK)

		/**
		 * Shortcut used for changing the subtitle track
		 */
		var shortcutSubtitleTrack = Preference.int("shortcut_subtitle_track", KeyEvent.KEYCODE_CAPTIONS)

		/* ACRA */
		/**
		 * Enable ACRA crash reporting
		 */
		var acraEnabled = Preference.boolean(ACRA.PREF_ENABLE_ACRA, true)

		/**
		 * Never prompt to report crash logs
		 */
		var acraNoPrompt = Preference.boolean(ACRA.PREF_ALWAYS_ACCEPT, false)

		/**
		 * Include system logs in crash reports
		 */
		var acraIncludeSystemLogs = Preference.boolean(ACRA.PREF_ENABLE_SYSTEM_LOGS, true)

		/**
		 * When to show the clock.
		 */
		var clockBehavior = Preference.enum("pref_clock_behavior", ClockBehavior.ALWAYS)

		/**
		 * Set which ratings provider should show on MyImageCardViews
		 */
		var defaultRatingType = Preference.enum("pref_rating_type", RatingType.RATING_TOMATOES)

		/**
		 * Set when watched indicators should show on MyImageCardViews
		 */
		var watchedIndicatorBehavior = Preference.enum("pref_watched_indicator_behavior", WatchedIndicatorBehavior.ALWAYS)

		/**
		 * Enable series thumbnails in home screen rows
		 */
		var seriesThumbnailsEnabled = Preference.boolean("pref_enable_series_thumbnails", true)
	}

	init {
		// Migrations
		// v0.10.x to v0.11.x: Old migrations
		migration(toVersion = 2) {
			// Migrate to video player enum
			// Note: This is the only time we need to check if the value is not set yet because the version numbers were reset
			if (!it.contains("video_player"))
				putEnum("video_player", if (it.getBoolean("pref_video_use_external", false)) PreferredVideoPlayer.EXTERNAL else PreferredVideoPlayer.AUTO)
		}

		// v0.11.x to v0.12.x: Migrates from the old way of storing preferences to the current
		migration(toVersion = 3) {
			// Migrate to audio behavior enum
			putEnum("audio_behavior", if (it.getString("pref_audio_option", "0") == "1") AudioBehavior.DOWNMIX_TO_STEREO else AudioBehavior.DIRECT_STREAM)

			// Migrate live tv player to use enum
			putEnum("live_tv_video_player",
				when {
					it.getBoolean("pref_live_tv_use_external", false) -> PreferredVideoPlayer.EXTERNAL
					it.getBoolean("pref_enable_vlc_livetv", false) -> PreferredVideoPlayer.VLC
					else -> PreferredVideoPlayer.AUTO
				})
		}

		// Change audio delay type from long to int
		migration(toVersion = 4) {
			putInt("libvlc_audio_delay", it.getLong("libvlc_audio_delay", 0).toInt())
		}
	}
}
