package org.jellyfin.androidtv.preferences.enums

enum class PreferredVideoPlayer {
	/**
	 *  Automatically selects between exoplayer and vlc
	 */
	AUTO,

	/**
	 *  exoplayer: Force ExoPlayer
	 */
	EXOPLAYER,

	/**
	 * Force libVLC
	 */
	VLC,

	/**
	 * Use external player
	 */
	EXTERNAL
}
