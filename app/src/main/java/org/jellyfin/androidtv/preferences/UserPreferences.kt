package org.jellyfin.androidtv.preferences

import android.content.Context
import androidx.preference.PreferenceManager
import org.jellyfin.androidtv.preferences.enums.AudioBehavior
import org.jellyfin.androidtv.preferences.enums.LoginBehavior
import org.jellyfin.androidtv.preferences.enums.PreferredVideoPlayer

/**
 * User preferences are configurable by the user and change behavior of the application.
 * When changing preferences migration should be added to the init function.
 *
 * @param context Context to get the SharedPreferences from
 */
class UserPreferences(context: Context) : SharedPreferenceStore(PreferenceManager.getDefaultSharedPreferences(context)) {
	/* Authentication */
	/**
	 * Behavior for login when starting the app.
	 * **note**: Currently settable via user-preferences only due too custom logic
	 */
	var loginBehavior by enumPreference("login_behavior", LoginBehavior.SHOW_LOGIN)
		private set

	/**
	 * Ask for password when starting the app
	 */
	var passwordPromptEnabled by booleanPreference("pref_auto_pw_prompt", false)

	/**
	 * Use login using pin (when set)
	 */
	var passwordDPadEnabled by booleanPreference("pref_alt_pw_entry", false)

	/* Display */
	/**
	 * Enable background images while browsing
	 */
	var backdropEnabled by booleanPreference("pref_show_backdrop", true)

	/**
	 * Show additional information for selected item while browsing
	 */
	var infoPanelEnabled by booleanPreference("pref_enable_info_panel", false)

	/**
	 * Show premieres on home screen
	 */
	var premieresEnabled by booleanPreference("pref_enable_premieres", false)

	/**
	 * Show a little notification to celebrate a set of holidays
	 */
	var seasonalGreetingsEnabled by booleanPreference("pref_enable_themes", true)

	/**
	 * Show additional debug information
	 */
	var debuggingEnabled by booleanPreference("pref_enable_debug", false)

	/* Playback - General*/
	/**
	 * Maximum bitrate in megabit for playback. A value of 0 means "auto".
	 */
	var maxBitrate by stringPreference("pref_max_bitrate", "0")

	/**
	 * Auto-play next item
	 */
	var mediaQueuingEnabled by booleanPreference("pref_enable_tv_queuing", true)

	/**
	 * Enable the next up screen or not
	 */
	var nextUpEnabled by booleanPreference("next_up_enabled", true)

	/**
	 * Next up timeout before playing next item
	 * Stored in milliseconds
	 */
	var nextUpTimeout by intPreference("next_up_timeout", 1000 * 7)

	/**
	 * Duration in seconds to subtract from resume time
	 */
	var resumeSubtractDuration by stringPreference("pref_resume_preroll", "0")

	/**
	 * Enable cinema mode
	 */
	var cinemaModeEnabled by booleanPreference("pref_enable_cinema_mode", true)

	/* Playback - Video */
	/**
	 * Preferred video player.
	 */
	var videoPlayer by enumPreference("video_player", PreferredVideoPlayer.AUTO)

	/**
	 * Enable refresh rate switching when device supports it
	 */
	var refreshRateSwitchingEnabled by booleanPreference("pref_refresh_switching", false)

	/**
	 * Send a path instead to the external player
	 */
	var externalVideoPlayerSendPath by booleanPreference("pref_send_path_external", false)

	/* Playback - Audio related */
	/**
	 * Preferred behavior for audio streaming.
	 */
	var audioBehaviour by enumPreference("audio_behavior", AudioBehavior.DIRECT_STREAM)

	/**
	 * Enable DTS
	 */
	var dtsEnabled by booleanPreference("pref_bitstream_dts", false)

	/**
	 * Enable AC3
	 */
	var ac3Enabled by booleanPreference("pref_bitstream_ac3", true)

	/**
	 * Default audio delay in milliseconds for libVLC
	 */
	var libVLCAudioDelay by longPreference("libvlc_audio_delay", 0)

	/* Live TV */
	/**
	 * Open live tv when opening the app
	 */
	var liveTvMode by booleanPreference("pref_live_tv_mode", false)

	/**
	 * Use direct play
	 */
	var liveTvDirectPlayEnabled by booleanPreference("pref_live_direct", true)

	/**
	 * Preferred video player for live TV
	 */
	var liveTvVideoPlayer by enumPreference("live_tv_video_player", PreferredVideoPlayer.AUTO)

	/* ACRA */
	/**
	 * Enable ACRA crash reporting
	 */
	var acraEnabled by booleanPreference("acra.enable", true)

	/**
	 * Never prompt to report crash logs
	 */
	var acraNoPrompt by booleanPreference("acra.alwaysaccept", false)

	/**
	 * Include system logs in crash reports
	 */
	var acraIncludeSystemLogs by booleanPreference("acra.syslog.enable", true)

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

			// Migrate to login behavior enum
			putEnum("login_behavior", if (it.getString("pref_login_behavior", "0") == "1") LoginBehavior.AUTO_LOGIN else LoginBehavior.SHOW_LOGIN)

			// Migrate live tv player to use enum
			putEnum("live_tv_video_player",
				when {
					it.getBoolean("pref_live_tv_use_external", false) -> PreferredVideoPlayer.EXTERNAL
					it.getBoolean("pref_enable_vlc_livetv", false) -> PreferredVideoPlayer.VLC
					else -> PreferredVideoPlayer.AUTO
				})
		}
	}
}
