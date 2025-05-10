package org.jellyfin.androidtv.data.service.themesong

import android.content.Context
import androidx.annotation.MainThread
import org.jellyfin.androidtv.ui.playback.rewrite.RewriteMediaManager
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ExtraType
import java.util.UUID

/**
 * Handles playing and stopping theme songs upon request. Not all play requests result in actual playback. Specifically, there will be no
 * playback if:
 * - Another audio media (that doesn't have the [ExtraType.THEME_SONG] type) is queued or currently playing.
 * - There are multiple sequential play requests for the same theme. Only the first request is honoured. Subsequent play requests for the
 *   same theme song will only work if a different theme song is played next, or if [stopThemeSong] is called.
 */
class ThemeSongPlayer(
	private val context: Context,
	private val mediaManager: RewriteMediaManager
) {
	private var allowedToPlay = false
	private var lastPlayedThemeSongUuid: UUID? = null

	fun prepareForPlay() {
		allowedToPlay = true
	}

	@MainThread
	fun playThemeSong(themeSong: BaseItemDto?) {
		if (nonThemeMusicPlayingOrQueued())
			return

		// Item doesn't have a theme song. Stop any already playing theme songs.
		if (themeSong == null || themeSong.extraType != ExtraType.THEME_SONG) {
			return stopThemeSong()
		}

		// Theme song is already playing or was the last thing played. Don't start it again.
		if (lastPlayedThemeSongUuid == themeSong.id) {
			return
		}

		if (allowedToPlay) {
			lastPlayedThemeSongUuid = themeSong.id
			mediaManager.playNow(context, listOf(themeSong), 0, false)
		}
	}

	@MainThread
	fun stopThemeSong() {
		allowedToPlay = false
		lastPlayedThemeSongUuid = null
		if (mediaManager.currentAudioItem?.extraType == ExtraType.THEME_SONG)
			mediaManager.stopAudio(true)
	}

	private fun nonThemeMusicPlayingOrQueued(): Boolean {
		return ((mediaManager.isPlayingAudio && mediaManager.currentAudioItem?.extraType != ExtraType.THEME_SONG) ||
			(!mediaManager.isPlayingAudio && mediaManager.hasAudioQueueItems()))
	}

}
