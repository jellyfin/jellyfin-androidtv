package org.jellyfin.androidtv.preferences

import android.content.Context
import android.preference.PreferenceManager

class UserPreferences(context: Context) : SharedPreferenceStore(PreferenceManager.getDefaultSharedPreferences(context)) {
	/* Authentication */
	var loginBehavior by stringPreference("pref_login_behavior", "0")
	var passwordPromptEnabled by booleanPreference("pref_auto_pw_prompt", false)
	var passwordDPadEnabled by booleanPreference("pref_alt_pw_entry", false)
	var autoSignoutTimeout by stringPreference("pref_auto_logoff_timeout", "3600000")

	/* Display */
	var backdropEnabled by booleanPreference("pref_show_backdrop", false)
	var infoPanelEnabled by booleanPreference("pref_enable_info_panel", false)
	var premieresEnabled by booleanPreference("pref_enable_premieres", false)
	var seasonalGreetingsEnabled by booleanPreference("pref_enable_themes", false)
	var debuggingEnabled by booleanPreference("pref_enable_debug", false)

	/* Playback - General*/
	var maxBitrate by stringPreference("pref_max_bitrate", "0")
	var mediaQueuingEnabled by booleanPreference("pref_enable_tv_queuing", false)
	var resumePreroll by stringPreference("pref_resume_preroll", "0")
	var cinemaModeEnabled by booleanPreference("pref_enable_cinema_mode", false)

	/* Playback - Video */
	var videoPlayer by stringPreference("pref_video_player", "auto")
	var refreshRateSwitchingEnabled by booleanPreference("pref_refresh_switching", false)
	var externalVideoPlayerSendPath by booleanPreference("pref_send_path_external", false)

	/* Playback - Audio related */
	var audioOption by stringPreference("pref_audio_option", "0")
	var dtsEnabled by booleanPreference("pref_bitstream_dts", false)
	var ac3Enabled by booleanPreference("pref_bitstream_ac3", false)

	/* Live TV */
	var liveTvMode by booleanPreference("pref_live_tv_mode", false) // Open live tv when opening the app
	var liveTvDirectPlayEnabled by booleanPreference("pref_live_direct", false)
	var liveTvUseVlc by booleanPreference("pref_enable_vlc_livetv", false)
	var liveTvUseExternalPlayer by booleanPreference("pref_live_tv_use_external", false)

	/* ACRA */
	var acraEnabled by booleanPreference("acra.enable", false)
	var acraNoPrompt by booleanPreference("acra.alwaysaccept", false)
	var acraIncludeSystemLogs by booleanPreference("acra.syslog.enable", true)

	init {
		migration(toVersion = 2) {
			// Migrate to new player preference
			val useExternal = it.getBoolean("pref_video_use_external", false)
			putString("pref_video_player", if (useExternal) "external" else "auto")
		}
	}
}
