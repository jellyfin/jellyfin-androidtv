package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class PreferredVideoPlayer(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 *  Force ExoPlayer
	 */
	EXOPLAYER(R.string.pref_video_player_exoplayer),

	/**
	 * Force libVLC
	 */
	VLC(R.string.pref_video_player_vlc),

	/**
	 * Use external player
	 */
	EXTERNAL(R.string.pref_video_player_external),

	/**
	 * Choose a player - play with button
	 */
	CHOOSE(R.string.pref_video_player_choose),
}
