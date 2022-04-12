package org.jellyfin.androidtv.preference

import android.content.Context
import android.view.KeyEvent
import androidx.preference.PreferenceManager
import org.acra.ACRA
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer
import org.jellyfin.androidtv.preference.constant.RatingType
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior
import org.jellyfin.androidtv.preference.constant.defaultAudioBehavior
import org.jellyfin.androidtv.util.DeviceUtils
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.enumPreference
import org.jellyfin.preference.intPreference
import org.jellyfin.preference.migration.putEnum
import org.jellyfin.preference.store.SharedPreferenceStore
import org.jellyfin.preference.stringPreference

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
		/**
		 * The value used for automatic detection in [maxBitrate].
		 */
		const val MAX_BITRATE_AUTO = "0"

		/* Display */
		/**
		 * Select the app theme
		 */
		var appTheme = enumPreference("app_theme", AppTheme.DARK)

		/**
		 * Enable background images while browsing
		 */
		var backdropEnabled = booleanPreference("pref_show_backdrop", true)

		/**
		 * Show premieres on home screen
		 */
		var premieresEnabled = booleanPreference("pref_enable_premieres", false)

		/**
		 * Show a little notification to celebrate a set of holidays
		 */
		var seasonalGreetingsEnabled = booleanPreference("pref_enable_themes", true)

		/* Playback - General*/
		/**
		 * Maximum bitrate in megabit for playback. A value of [MAX_BITRATE_AUTO] is used when
		 * the bitrate should be automatically detected.
		 */
		var maxBitrate = stringPreference("pref_max_bitrate", "100")

		/**
		 * Auto-play next item
		 */
		var mediaQueuingEnabled = booleanPreference("pref_enable_tv_queuing", true)

		/**
		 * Enable the next up screen or not
		 */
		var nextUpBehavior = enumPreference("next_up_behavior", NextUpBehavior.EXTENDED)

		/**
		 * Next up timeout before playing next item
		 * Stored in milliseconds
		 */
		var nextUpTimeout = intPreference("next_up_timeout", 1000 * 7)

		/**
		 * Duration in seconds to subtract from resume time
		 */
		var resumeSubtractDuration = stringPreference("pref_resume_preroll", "0")

		/**
		 * Enable cinema mode
		 */
		var cinemaModeEnabled = booleanPreference("pref_enable_cinema_mode", true)

		/* Playback - Video */
		/**
		 * Preferred video player.
		 */
		var videoPlayer = enumPreference("video_player", PreferredVideoPlayer.EXOPLAYER)

		/**
		 * Enable refresh rate switching when device supports it
		 */
		var refreshRateSwitchingEnabled = booleanPreference("pref_refresh_switching", false)

		/**
		 * Send a path instead to the external player
		 */
		var externalVideoPlayerSendPath = booleanPreference("pref_send_path_external", false)

		/* Playback - Audio related */
		/**
		 * Preferred behavior for audio streaming.
		 */
		var audioBehaviour = enumPreference("audio_behavior", defaultAudioBehavior)

		/**
		 * Enable DTS
		 */
		var dtsEnabled = booleanPreference("pref_bitstream_dts", false)

		/**
		 * Enable AC3
		 */
		var ac3Enabled = booleanPreference("pref_bitstream_ac3", !DeviceUtils.isFireTvStickGen1())

		/**
		 * Default audio delay in milliseconds for libVLC
		 */
		var libVLCAudioDelay = intPreference("libvlc_audio_delay", 0)

		/* Live TV */
		/**
		 * Use direct play
		 */
		var liveTvDirectPlayEnabled = booleanPreference("pref_live_direct", true)

		/**
		 * Preferred video player for live TV
		 */
		var liveTvVideoPlayer = enumPreference("live_tv_video_player", PreferredVideoPlayer.AUTO)

		/**
		 * Shortcut used for changing the audio track
		 */
		var shortcutAudioTrack = intPreference("shortcut_audio_track", KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK)

		/**
		 * Shortcut used for changing the subtitle track
		 */
		var shortcutSubtitleTrack = intPreference("shortcut_subtitle_track", KeyEvent.KEYCODE_CAPTIONS)

		/* Developer options */
		/**
		 * Show additional debug information
		 */
		var debuggingEnabled = booleanPreference("pref_enable_debug", false)

		/**
		 * Use playback rewrite module
		 */
		var playbackRewriteEnabled = booleanPreference("playback_new", false)

		/* ACRA */
		/**
		 * Enable ACRA crash reporting
		 */
		var acraEnabled = booleanPreference(ACRA.PREF_ENABLE_ACRA, true)

		/**
		 * Never prompt to report crash logs
		 */
		var acraNoPrompt = booleanPreference(ACRA.PREF_ALWAYS_ACCEPT, false)

		/**
		 * Include system logs in crash reports
		 */
		var acraIncludeSystemLogs = booleanPreference(ACRA.PREF_ENABLE_SYSTEM_LOGS, true)

		/**
		 * When to show the clock.
		 */
		var clockBehavior = enumPreference("pref_clock_behavior", ClockBehavior.ALWAYS)

		/**
		 * Set which ratings provider should show on MyImageCardViews
		 */
		var defaultRatingType = enumPreference("pref_rating_type", RatingType.RATING_TOMATOES)

		/**
		 * Set when watched indicators should show on MyImageCardViews
		 */
		var watchedIndicatorBehavior = enumPreference("pref_watched_indicator_behavior", WatchedIndicatorBehavior.ALWAYS)

		/**
		 * Enable series thumbnails in home screen rows
		 */
		var seriesThumbnailsEnabled = booleanPreference("pref_enable_series_thumbnails", true)

		/**
		 * Enable subtitles background
		 */
		var subtitlesBackgroundEnabled = booleanPreference("subtitles_background_enabled", true)

		/**
		 * Set default subtitles font size
		 */
		var defaultSubtitlesSize = intPreference("subtitles_size", 28)
	}

	init {
		// Note: Create a single migration per app version
		// Note: Migrations are never executed for fresh installs
		runMigrations {
			// v0.10.x to v0.11.x
			migration(toVersion = 2) {
				// Migrate to video player enum
				// Note: This is the only time we need to check if the value is not set yet because the version numbers were reset
				if (!it.contains("video_player"))
					putEnum("video_player", if (it.getBoolean("pref_video_use_external", false)) PreferredVideoPlayer.EXTERNAL else PreferredVideoPlayer.AUTO)
			}

			// v0.11.x to v0.12.x
			migration(toVersion = 5) {
				// Migrate to audio behavior enum
				putEnum("audio_behavior", if (it.getString("pref_audio_option", "0") == "1") AudioBehavior.DOWNMIX_TO_STEREO else AudioBehavior.DIRECT_STREAM)

				// Migrate live tv player to use enum
				putEnum("live_tv_video_player",
					when {
						it.getBoolean("pref_live_tv_use_external", false) -> PreferredVideoPlayer.EXTERNAL
						it.getBoolean("pref_enable_vlc_livetv", false) -> PreferredVideoPlayer.VLC
						else -> PreferredVideoPlayer.AUTO
					})

				// Change audio delay type from long to int
				putInt("libvlc_audio_delay", it.getLong("libvlc_audio_delay", 0).toInt())

				// Disable AC3 (Dolby Digital) on Fire Stick Gen 1 devices
				if (DeviceUtils.isFireTvStickGen1()) putBoolean("pref_bitstream_ac3", false)
			}
		}
	}
}
