package org.jellyfin.androidtv.preference

import android.content.Context
import android.view.KeyEvent
import androidx.preference.PreferenceManager
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer
import org.jellyfin.androidtv.preference.constant.RatingType
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior
import org.jellyfin.androidtv.preference.constant.defaultAudioBehavior
import org.jellyfin.androidtv.util.DeviceUtils
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.enumPreference
import org.jellyfin.preference.intPreference
import org.jellyfin.preference.longPreference
import org.jellyfin.preference.store.SharedPreferenceStore
import org.jellyfin.preference.stringPreference
import kotlin.time.Duration.Companion.minutes

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
		 * Enable management of media like deleting items when the user has sufficient permisisons.
		 */
		var mediaManagementEnabled = booleanPreference("enable_media_management", false)

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
		 * Change refresh rate to match media when device supports it
		 */
		var refreshRateSwitchingBehavior = enumPreference("refresh_rate_switching_behavior", RefreshRateSwitchingBehavior.DISABLED)

		/* Playback - Audio related */
		/**
		 * Preferred behavior for audio streaming.
		 */
		var audioBehaviour = enumPreference("audio_behavior", defaultAudioBehavior)

		/**
		 * Preferred behavior for audio streaming.
		 */
		var audioNightMode = enumPreference("audio_night_mode", false)

		/**
		 * Enable DTS
		 */
		var dtsEnabled = booleanPreference("pref_bitstream_dts", false)

		/**
		 * Enable AC3
		 */
		var ac3Enabled = booleanPreference("pref_bitstream_ac3", !DeviceUtils.isFireTvStickGen1)

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
		 * Use playback rewrite module for video
		 */
		var playbackRewriteVideoEnabled = booleanPreference("playback_new", false)

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
		 * Subtitles font size
		 */
		var subtitlesSize = intPreference("subtitles_size", 28)

		/**
		 * Subtitles stroke size
		 */
		var subtitleStrokeSize = intPreference("subtitles_stroke_size", 0)

		/**
		 * Subtitles position
		 */
		var subtitlePosition = intPreference("subtitles_position", 40)

		/**
		 * Subtitles foreground color
		 */
		var subtitlesTextColor = longPreference("subtitles_text_color", 0xFFFFFFFF)

		/**
		 * Show screensaver in app
		 */
		var screensaverInAppEnabled = booleanPreference("screensaver_inapp_enabled", true)

		/**
		 * Timeout before showing the screensaver in app, depends on [screensaverInAppEnabled].
		 */
		var screensaverInAppTimeout = longPreference("screensaver_inapp_timeout", 5.minutes.inWholeMilliseconds)

		/**
		 * Enable reactive homepage
		 */
		var homeReactive = booleanPreference("home_reactive", false)
	}

	init {
		// Note: Create a single migration per app version
		// Note: Migrations are never executed for fresh installs
		// Note: Old migrations are removed occasionally
		runMigrations {
			// v0.15.z to v0.16.0
			migration(toVersion = 7) {
				// Enable playback rewrite for music
				putBoolean("playback_new_audio", true)
			}
		}
	}
}
