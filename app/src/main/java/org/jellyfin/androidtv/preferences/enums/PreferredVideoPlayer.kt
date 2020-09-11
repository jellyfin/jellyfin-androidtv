package org.jellyfin.androidtv.preferences.enums

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preferences.dsl.EnumDisplayOptions

enum class PreferredVideoPlayer {
	/**
	 *  Automatically selects between exoplayer and vlc
	 */
	@EnumDisplayOptions(R.string.pref_video_player_auto)
	AUTO,

	/**
	 *  Force ExoPlayer
	 */
	@EnumDisplayOptions(R.string.pref_video_player_exoplayer)
	EXOPLAYER,

	/**
	 * Force libVLC
	 */
	@EnumDisplayOptions(R.string.pref_video_player_vlc)
	VLC,

	/**
	 * Use external player
	 */
	@EnumDisplayOptions(R.string.pref_video_player_external)
	EXTERNAL
}
