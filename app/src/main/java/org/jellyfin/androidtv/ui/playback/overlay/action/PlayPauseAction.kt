package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import androidx.leanback.widget.PlaybackControlsRow
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class PlayPauseAction(context: Context) : PlaybackControlsRow.PlayPauseAction(context),
	AndroidAction {
	override fun onActionClicked(videoPlayerAdapter: VideoPlayerAdapter) =
		when (this.index) {
			INDEX_PLAY -> {
				videoPlayerAdapter.play()
				this.index = INDEX_PAUSE
			}

			INDEX_PAUSE -> {
				videoPlayerAdapter.pause()
				this.index = INDEX_PLAY
			}

			else -> {}
		}

}
