package org.jellyfin.androidtv.ui.playback.theme

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class ThemeAudioPlayer(private val context: Context) {
	private var exoPlayer: ExoPlayer? = null

	fun play(url: String) {
		if (exoPlayer == null) {
			exoPlayer = ExoPlayer.Builder(context).build().apply {
				volume = 0.25f // Not too loud + Maybe a setting later
				repeatMode = Player.REPEAT_MODE_ONE
			}
		}

		exoPlayer?.apply {
			setMediaItem(MediaItem.fromUri(url))
			prepare()
			playWhenReady = true
		}
	}

	fun stop() {
		exoPlayer?.stop()
		exoPlayer?.clearMediaItems()
	}

	fun release() {
		exoPlayer?.release()
		exoPlayer = null
	}
}
